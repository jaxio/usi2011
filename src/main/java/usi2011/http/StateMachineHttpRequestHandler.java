package usi2011.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpMethod.POST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.REQUEST_ALLOWED;
import static usi2011.util.FastIntegerParser.isNumber;
import static usi2011.util.FastIntegerParser.parseInt;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;
import static usi2011.util.Specifications.MAX_POSSIBLE_SCORE;
import static usi2011.util.Specifications.URI_ALIVE;
import static usi2011.util.Specifications.URI_ANSWER;
import static usi2011.util.Specifications.URI_AUDIT;
import static usi2011.util.Specifications.URI_GAME;
import static usi2011.util.Specifications.URI_HOSTNAME;
import static usi2011.util.Specifications.URI_LOGIN;
import static usi2011.util.Specifications.URI_QUESTION;
import static usi2011.util.Specifications.URI_RANKING;
import static usi2011.util.Specifications.URI_SCORE;
import static usi2011.util.Specifications.URI_USER;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import me.prettyprint.hector.api.exceptions.HectorException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import usi2011.domain.Answer;
import usi2011.domain.Parameters;
import usi2011.domain.Question;
import usi2011.domain.Ranking;
import usi2011.domain.Ranking.UserScore;
import usi2011.domain.Session;
import usi2011.domain.User;
import usi2011.domain.UserScoreHistory;
import usi2011.exception.AuthenticationKeyException;
import usi2011.exception.UsiException;
import usi2011.http.encoder.HttpResponseEncoderUtil;
import usi2011.http.parser.AnswerParser;
import usi2011.http.parser.LoginParser;
import usi2011.http.parser.ScoreAndAuditParser;
import usi2011.http.support.CookieService;
import usi2011.http.support.HttpResponseService;
import usi2011.http.support.HttpServeFile;
import usi2011.repository.DoubleLoginRepository;
import usi2011.repository.GameStatusRepository;
import usi2011.repository.LoginStatusRepository;
import usi2011.repository.ParameterRepository;
import usi2011.repository.QuestionRepository;
import usi2011.repository.ScoreRepository;
import usi2011.repository.UserRepository;
import usi2011.service.RankingService;
import usi2011.statemachine.StateMachine;
import usi2011.statemachine.StateMachine.CurrentState;
import usi2011.statemachine.StateMachine.RequestQuestionAuthorization;
import usi2011.statemachine.support.StateCallback;
import usi2011.task.BatchRankingPublisherTask;
import usi2011.task.BatchScoreUpdateTask;
import usi2011.util.NamedThreadFactory;

@Component
public class StateMachineHttpRequestHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = getLogger(StateMachineHttpRequestHandler.class);
    @Autowired
    private StateMachine stateMachine;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginStatusRepository loginStatusRepository;
    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    private DoubleLoginRepository doubleLoginRepository;
    @Autowired
    private BatchScoreUpdateTask updateScoreTask;
    @Autowired
    private BatchRankingPublisherTask rankingTask;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ParameterRepository parameterRepository;
    @Autowired
    private GameStatusRepository gameStatusRepository;
    @Autowired
    private HttpServeFile httpServeFile;
    @Autowired
    private CookieService cookieService;
    @Autowired
    private RankingService rankingService;
    @Autowired
    private HttpResponseService httpResponseService;
    @Value("${enable.questionBatchChannelAndResponse}")
    private boolean enableQuestionBatchChannelAndResponse;
    @Value("${use.precomputedQuestions}")
    private boolean usePrecomputedQuestions;
    
    @Value("${enforce.login.uniqueness}")
    private boolean enforceLoginUniqueness;

    private static final String hostname = buildHostname();
    private static String buildHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // use another thread to update login statuses
    private final ExecutorService loginStatusService = newSingleThreadScheduledExecutor(new NamedThreadFactory("Saving in "
            + LoginStatusRepository.class.getSimpleName()));

    private Question[] questions;
    private byte[][][] questionsScoreHttpResponseHTTP1_1 = new byte[MAX_NUMBER_OF_QUESTIONS + 1][MAX_POSSIBLE_SCORE + 1][];
    private byte[][][] questionsScoreHttpResponseHTTP1_0 = new byte[MAX_NUMBER_OF_QUESTIONS + 1][MAX_POSSIBLE_SCORE + 1][];
    private Parameters parameters;
    private int[] goodAnswers;

    public void init() {
        if (isInfoEnabled) {
            logger.info("initializing...");
        }
        parameters = parameterRepository.getParameters();
        if (parameters != null) {
            if (isInfoEnabled) {
                logger.info("We have some parameters. Nb of questions={}", parameters.getNbQuestions());
            }

            questions = new Question[parameters.getNbQuestions() + 1];
            goodAnswers = new int[MAX_NUMBER_OF_QUESTIONS + 1];

            for (int questionId = 1; questionId <= parameters.getNbQuestions(); questionId++) {
                questions[questionId] = questionRepository.getQuestion(questionId);
                goodAnswers[questionId] = questions[questionId].getGoodChoice();

                for (int score = 0; score <= MAX_POSSIBLE_SCORE; score++) {
                    questionsScoreHttpResponseHTTP1_0[questionId][score] = buildScoreHttpResponse(questionId, score, 0);
                    questionsScoreHttpResponseHTTP1_1[questionId][score] = buildScoreHttpResponse(questionId, score, 1);
                }
            }
        } else {
            if (isInfoEnabled) {
                logger.info("No parameters found");
            }
        }
    }

    private byte[] buildScoreHttpResponse(int questionId, int score, int httpVersion) {
        final String question = questions[questionId].asJsonStringForScore(score);
        final String build = "HTTP/1." + httpVersion + " 200 OK\r\n" + //
                "Content-Type: application/json\r\n" + //
                "Content-Length:" + question.length() + "\r\n" + //
                "\r\n" //
                + question;
        return build.getBytes();
    }

    /**
     * Main entry point for client requests.
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final HttpRequest request = (HttpRequest) e.getMessage();
        if (isDebugEnabled) {
            logger.debug("[{}:{}]", request.getMethod().getName(), request.getUri());
        }
        try {
            if (POST == request.getMethod()) {
                post(e, request);
            } else if (GET == request.getMethod()) {
                get(ctx, e, request);
            } else {
                httpResponseService.writeResponse(e, request, METHOD_NOT_ALLOWED, request.getMethod().getName() + " method is not authorized");
            }
        } catch (AuthenticationKeyException authex) {
            httpResponseService.writeResponse(e, request, authex.getHttpResponseStatus(), authex.getMessage());
        } catch (UsiException exception) {
            httpResponseService.writeResponse(e, request, exception.getHttpResponseStatus(), exception);
        } catch (HectorException exception) {
            if (isWarnEnabled) {
                logger.warn("Hector exception", exception);
            }
            httpResponseService.writeResponse(e, request, INTERNAL_SERVER_ERROR, exception);
        } catch (Exception exception) {
            httpResponseService.writeResponse(e, request, INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void get(final ChannelHandlerContext ctx, final MessageEvent e, final HttpRequest request) {
        final String uri = request.getUri();
        if (uri.startsWith(URI_QUESTION)) {
            question(ctx, e, request);
        } else if (URI_RANKING.equals(uri)) {
            ranking(e, request);
        } else if (uri.startsWith(URI_SCORE)) {
            score(e, request);
        } else if (uri.startsWith("/api/audit?")) {
            audit(e, request);
        } else if (uri.startsWith("/api/audit/")) {
            auditN(e, request);
        } else if (uri.startsWith(URI_ALIVE)) {
            httpResponseService.writeResponse(e, request, OK, "alive");
        } else if (uri.startsWith(URI_HOSTNAME)) {
            httpResponseService.writeResponse(e, request, OK, hostname);
        } else if (!httpServeFile.serve(e, request)) {
            httpResponseService.writeResponse(e, request, NOT_FOUND, "unknown " + request.getUri());
        }
    }

    private void post(final MessageEvent e, final HttpRequest request) {
        final String uri = request.getUri();
        if (URI_USER.equals(uri)) {
            user(e, request);
        } else if (URI_GAME.equals(uri)) {
            game(e, request);
        } else if (URI_LOGIN.equals(uri)) {
            login(e, request);
        } else if (uri.startsWith(URI_ANSWER)) {
            answer(e, request);
        } else {
            httpResponseService.writeResponse(e, request, NOT_FOUND, "unknown POST method " + uri);
        }
    }

    private void user(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isUserAllowed()) {
            if (userRepository.save(new User(getRequestString(request)))) {
                httpResponseService.writeResponse(e, request, CREATED, "ok");
            } else {
                httpResponseService.writeResponse(e, request, BAD_REQUEST, "user already exist");
            }
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    private void game(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isGameAllowed()) {
            if (isInfoEnabled) {
                logger.info("This server will create a new game");
            }
            // NOTE: In this method, you MUST NOT do local stuff such as invoking init() or
            // setting to null local values...
            // Indeed, you must THINK DISTRIBUTED as other servers won't receive this /api/game request.
            // The init() method will be invoked during the state machine's onGameStatus()
            Session session = new Session(getRequestString(request));
            loginStatusRepository.reset(); // OK here since it is a truncate
            questionRepository.save(session); // OK here since we save 1 time
            gameStatusRepository.reset(); // OK here since it is a truncate
            // TODO: à faire selon les params, mais pour les tests, on vide tout..
            scoreRepository.reset(); // OK here since it is a truncate
            doubleLoginRepository.reset(); // OK here since it is a truncate

            try {
                if (isInfoEnabled) {
                    logger.info("Sleeping 2 seconds for the truncate to replicate");
                }
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignore) {
            }
            gameStatusRepository.gameCreated(currentTimeMillis());
            httpResponseService.writeResponse(e, request, CREATED, "game created with " + session.getQuestions().size() + " questions");
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    private void audit(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isAuditAllowed()) {
            final String email = new ScoreAndAuditParser(request.getUri()).getEmail();
            httpResponseService.writeResponse(e, request, OK, scoreRepository.getAudit(email, goodAnswers).getJson());
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    private void auditN(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isAuditAllowed()) {
            final String questionIdExtracted = getNumericSuffix(request, URI_AUDIT);
            if (!isNumber(questionIdExtracted)) {
                httpResponseService.writeResponse(e, request, BAD_REQUEST, "Could not get question Id, got " + questionIdExtracted);
                return;
            }
            final int questionId = parseInt(questionIdExtracted);
            final String email = new ScoreAndAuditParser(request.getUri()).getEmail();
            httpResponseService.writeResponse(e, request, OK, scoreRepository.getAudit(email, questions[questionId]).toJsonString());
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    private void score(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isScoreAllowed()) {
            final String email = new ScoreAndAuditParser(request.getUri()).getEmail();
            final User user = userRepository.get(email);
            if (user == null) {
                httpResponseService.writeResponse(e, request, NOT_FOUND, email + " could not be found");
            } else {
                final int score = scoreRepository.getScore(user, questions, goodAnswers);
                final Ranking ranking = rankingService.getRanking(new UserScore(score, user.getLastName(), user.getFirstName(), user.getEmail()));
                httpResponseService.writeResponse(e, request, OK, ranking.getJson());
            }
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    /**
     * A few steps are needed while login a player
     * <ol>
     * <li>Validate that the {@link StateMachine} allows users to log in</li>
     * <li>Retrieve the user corresponding to the email in the {@link UserRepository}</li>
     * <li>Check the password</li>
     * <li>Check if the user is not already logged somewhere else via the {@link LoginStatusRepository}</li>
     * <li>Send response with cookie to the user</li>
     * <li>Save the login status information in the {@link LoginStatusRepository}</li>
     * </ol>
     */
    private void login(final MessageEvent e, final HttpRequest request) {
        if (stateMachine.isLoginAllowed()) {
            final LoginParser login = new LoginParser(getRequestString(request));

            final User user = userRepository.get(login.getEmail());
            if (user == null) {
                httpResponseService.writeResponse(e, request, UNAUTHORIZED, "unknown user");
                if (isWarnEnabled) {
                    logger.warn("User {} is unknown!", login.getEmail());
                }
            } else if (login.getPassword().equals(user.getPassword())) {
                loginUser(e, request, user);
            } else {
                httpResponseService.writeResponse(e, request, UNAUTHORIZED, "invalid user or password");
                if (isWarnEnabled) {
                    logger.warn("User {} has invalid credential", login.getEmail());
                }
            }
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "no login allowed in the current state");
        }
    }

    private void loginUser(final MessageEvent e, final HttpRequest request, final User user) {
        if (enforceLoginUniqueness && doubleLoginRepository.isUserLoggedIn(user.getEmail())) {
            if (isWarnEnabled) {
                logger.warn("{} is already logged (DoubleLogin CF)", user.getEmail());
            }
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "user is already logged in somewhere else");
        } else {
            stateMachine.handleLogin();
            httpResponseService.writeResponseWithCookie(e, request, CREATED, (String) null, //
                    cookieService.set( //
                            UserScoreHistory.buildForLogin(user.getEmail(), user.getLastName(), user.getFirstName())));
            
            if (enforceLoginUniqueness) {
                // save in background the login status
                loginStatusService.execute(new Runnable() {
                    @Override
                    public void run() {
                        doubleLoginRepository.userLoggedIn(user.getEmail());
                    }
                });
            }
        }
    }

    private void question(final ChannelHandlerContext ctx, final MessageEvent e, final HttpRequest request) {
        final String questionIdExtracted = getNumericSuffix(request, URI_QUESTION);
        if (!isNumber(questionIdExtracted)) {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Could not get question Id, got " + questionIdExtracted);
            return;
        }
        final int questionId = parseInt(questionIdExtracted);
        final RequestQuestionAuthorization requestQuestion = stateMachine.isQuestionAllowed(questionId);

        final UserScoreHistory cookieValue = new UserScoreHistory(cookieService.get(request), goodAnswers);

        if (requestQuestion != REQUEST_ALLOWED) {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, requestQuestion.name());

            if (isWarnEnabled) {
                logger.warn("User {} could not reveal question {} because {}", new Object[] { cookieValue.getEmail(), questionId, requestQuestion.name() });
            }
            return;
        }
        if (usePrecomputedQuestions) {
            sendPrecomputedQuestions(e, request, questionId, cookieValue);
            return;
        }
        final Question question = questions[questionId];
        final String jsonContent = question.asJsonStringForScore(cookieValue.getScore(questionId));

        if (enableQuestionBatchChannelAndResponse) {
            final ChannelBuffer channelBuffer = wrappedBuffer(jsonContent.getBytes(UTF_8));
            final HttpResponse response = httpResponseService.response(request.getProtocolVersion(), OK);
            if (isInfoEnabled) {
                if (response.getStatus() != OK && response.getStatus() != CREATED) {
                    logger.info("[{}:{}] {} {}", new Object[] { request.getMethod().getName(), request.getUri(), response.getStatus().getCode(),
                            response.getStatus().getReasonPhrase() });
                }
            }
            final Channel channel = e.getChannel();
            if (channelBuffer != null) {
                response.setHeader(CONTENT_TYPE, "application/json");
                response.setContent(channelBuffer);
            }
            response.setHeader(CONTENT_LENGTH, "" + channelBuffer.readableBytes());

            // prepare in advance the work that is done when it is intercepted by the pipeline's HttpResponseEncoder
            final ChannelBuffer preparedResponse = HttpResponseEncoderUtil.prepareRawResponse(ctx, channel, response);

            stateMachine.question(questionId, new StateCallback() {
                @Override
                public void success() {
                    channel.write(preparedResponse);
                }

                @Override
                public void failure(String reason) {
                    if (isWarnEnabled) {
                        logger.warn("User {} could not reveal question {} because {}", new Object[] { cookieValue.getEmail(), questionId, reason });
                    }
                    httpResponseService.writeResponse(e, request, BAD_REQUEST, reason);
                }
            });

            
        } else {
            stateMachine.question(questionId, new StateCallback() {
                @Override
                public void success() {
                    httpResponseService.writeResponse(e, request, OK, jsonContent);
                }

                @Override
                public void failure(String reason) {
                    if (isWarnEnabled) {
                        logger.warn("User {} could not reveal question {} because {}", new Object[] { cookieValue.getEmail(), questionId, reason });
                    }
                    httpResponseService.writeResponse(e, request, BAD_REQUEST, reason);
                }
            });
        }
    }
    
    private void sendPrecomputedQuestions(final MessageEvent e, final HttpRequest request, final int questionId, final UserScoreHistory cookieValue) {
        final Channel channel = e.getChannel();
        final int score = cookieValue.getScore(questionId);
        final boolean http1_1 = request.getProtocolVersion() == HttpVersion.HTTP_1_1; 
        if (enableQuestionBatchChannelAndResponse) {
            stateMachine.question(questionId, new StateCallback() {
                @Override
                public void success() {
                    if (http1_1) {
                        channel.write(wrappedBuffer(questionsScoreHttpResponseHTTP1_1[questionId][score]));
                    } else {
                        channel.write(wrappedBuffer(questionsScoreHttpResponseHTTP1_0[questionId][score]));
                    }
                }

                @Override
                public void failure(String reason) {
                    if (isWarnEnabled) {
                        logger.warn("User {} could not reveal question {} because {}", new Object[] { cookieValue.getEmail(), questionId, reason });
                    }
                    httpResponseService.writeResponse(e, request, BAD_REQUEST, reason);
                }
            });
        } else {
            if (http1_1) {
                channel.write(wrappedBuffer(questionsScoreHttpResponseHTTP1_1[questionId][score]));
            } else {
                channel.write(wrappedBuffer(questionsScoreHttpResponseHTTP1_0[questionId][score]));
            }
        }
    }

    private void answer(final MessageEvent e, final HttpRequest request) {
        final String questionIdExtracted = getNumericSuffix(request, URI_ANSWER);
        final int questionId = parseInt(questionIdExtracted);
        final UserScoreHistory userScoreHistory = new UserScoreHistory(cookieService.get(request), goodAnswers);

        if (stateMachine.isAnswerAllowed(questionId)) {
            final AnswerParser answerCommand = new AnswerParser(getRequestString(request));
            // TODO Florent ===> quoi t'en penses? final AnswerParser answerCommand = new AnswerParser(getRequestBytes(request));
            final Question question = questions[questionId];
            final int userAnswer = answerCommand.getUserAnswer();

            if (userScoreHistory.userHasAlreadyAnsweredQuestion(question)) {
                if (isWarnEnabled) {
                    logger.warn("Email {} - answer to question {} has already been handled", new Object[] { userScoreHistory.getEmail(), questionId });
                }
                httpResponseService.writeResponse(e, request, BAD_REQUEST, "Answer to question " + questionId + " already handled");
            } else {
                userScoreHistory.answer(question, userAnswer);

                String answerJSON = Answer.toJSONString(question.getGoodAnswerJsonEscaped() //
                        , question.isRightAnswer(userAnswer)//
                        , userScoreHistory.getScore(question.getQuestionId()));

                httpResponseService.writeResponseWithCookie(e, request, CREATED, answerJSON, cookieService.set(userScoreHistory));

                // final score publish so ranking can be computed
                if (parameters.getNbQuestions() == questionId) {
                    rankingTask.publishRankingAsynch(userScoreHistory);
                }

                // publish score for this question
                updateScoreTask.updateScoreAsynch(userScoreHistory, question);
            }
        } else {
            CurrentState cs = stateMachine.getCurrentState();

            if (parameters.getNbQuestions() == questionId && cs.getState().isSYNCHROTIME()) {
                // last answer arrived too late, but we must still compute and publish the ranking as user is going to request it.
                rankingTask.publishRankingAsynch(userScoreHistory);
            }

            if (isWarnEnabled) {
                logger.warn("Email {} - answer to question {} is not accepted in {}({}) state", new Object[] { userScoreHistory.getEmail(), questionId,
                        cs.getState(), cs.getQuestionN()});
            }
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + cs);
        }
    }

    private void ranking(final MessageEvent e, final HttpRequest request) {
        final UserScoreHistory cookieValue = new UserScoreHistory(cookieService.get(request), goodAnswers);
        if (stateMachine.isRankingAllowed()) {
            httpResponseService.writeResponse(e, request, OK, //
                    rankingService.getRanking( //
                            new UserScore( //
                                    cookieValue.getScore(parameters.getNbQuestions()), //
                                    cookieValue.getLastName(), //
                                    cookieValue.getFirstName(), //
                                    cookieValue.getEmail())) //
                            .getJson());
            stateMachine.incrementRankingCounter();
        } else {
            httpResponseService.writeResponse(e, request, BAD_REQUEST, "Not the right state. Current state is " + stateMachine.getCurrentState());
        }
    }

    static final String getNumericSuffix(final HttpRequest request, final String URI) {
        return getNumericSuffix(request.getUri(), URI);
    }

    static final String getNumericSuffix(final String request, final String URI) {
        final int minSize = URI.length() + "/".length();
        if (request.length() < minSize) {
            return "";
        }
        final String ret = request.substring(minSize);
        final int exclamationPointPosition = ret.indexOf('?');
        return exclamationPointPosition == -1 ? ret : ret.substring(0, exclamationPointPosition);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent exception) {
        logger.error("Got exception {}, from remote address {}", exception.getCause().getMessage(), ctx.getChannel().getRemoteAddress());
        logger.error(exception.getCause().getMessage(), exception.getCause());

        if (ctx.getChannel().isBound() && ctx.getChannel().isWritable()) {
            String content = exception.getCause().getMessage();
            if (isDebugEnabled) {
                exception.getCause().printStackTrace();
                content += "\n" + getStackTrace(exception.getCause());
            }
            httpResponseService.write(ctx.getChannel(), BAD_REQUEST, false, content, "text/plain");
        }
    }

    private String getRequestString(HttpRequest request) {
        final ChannelBuffer content = request.getContent();
        final String requestString = content.readable() ? content.toString(UTF_8) : null;
        if (isDebugEnabled) {
            logger.debug("Request string:  {}", (requestString == null ? "empty" : requestString));
        }
        return requestString;
    }
}