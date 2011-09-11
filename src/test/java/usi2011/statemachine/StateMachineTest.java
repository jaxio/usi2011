package usi2011.statemachine;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static usi2011.Main.context;
import static usi2011.statemachine.StateMachine.State.LOGIN;
import static usi2011.statemachine.StateMachine.State.QUESTIONTIMEFRAME;
import static usi2011.statemachine.StateMachine.State.RANKING;
import static usi2011.statemachine.StateMachine.State.START;
import static usi2011.statemachine.StateMachine.State.SYNCHROTIME;
import static usi2011.util.Specifications.FIRST_QUESTION;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import usi2011.Main;
import usi2011.domain.GameStatus;
import usi2011.domain.Parameters;
import usi2011.domain.Parameters.ParametersBuilder;
import usi2011.repository.GameStatusRepository;
import usi2011.repository.ParameterRepository;
import usi2011.statemachine.StateMachine.CurrentState;
import usi2011.statemachine.StateMachine.State;
import usi2011.statemachine.support.CallbackFlusher;
import usi2011.statemachine.support.StateCallback;
import usi2011.task.PeriodicTask;

@Ignore
public class StateMachineTest {
    ParameterRepository parameterRepository;
    GameStatusRepository gameStatusRepository = null;
    StateMachineImpl impl;
    StateMachine usiMachine;
    PeriodicTask usiPeriodicTaskTarget;

    Parameters p = new ParametersBuilder() //
            .setNbUsersThreshold(5) //
            .setLoginTimeoutInSeconds(1)//
            .setQuestionTimeframeInSeconds(1) //
            .setSynchrotimeInSeconds(1) //
            .setFlushUserTable(false) //
            .setNbQuestions(20) //
            .build();

    @Before
    public void init() throws Exception {
        gameStatusRepository = context.getBean(GameStatusRepository.class);
        gameStatusRepository.reset();
        impl = Main.context.getBean(StateMachineImpl.class);

        usiPeriodicTaskTarget = (PeriodicTask) impl;

        impl.callbackFlusher = new CallbackFlusher() {
            @Override
            public void asyncSuccess(LinkedBlockingQueue<StateCallback> callbacks, int questionId) {
            }
        };
        
        // wire mock parameterRepo
        parameterRepository = mock(ParameterRepository.class);
        when(parameterRepository.getParameters()).thenReturn(p);
        impl.parameterRepository = parameterRepository;
        
        usiMachine = (StateMachine) impl;
        usiMachine.reset();
        usiMachine.initGame(1234);

        assertThat(usiMachine.getCurrentState().getState()).isEqualTo(START);
        assertThat(usiMachine.getCurrentState().getQuestionN()).isEqualTo(1);
        assertThat(usiMachine.getCurrentState().getState().isSTART()).isTrue();
    }

    @Test
    public void getCurrentStateFromTQuestion1() {
        long TQ1 = currentTimeMillis();
        CurrentState cs = impl.getCurrentStateFromTQuestion1(TQ1);
        assertThat(cs.getState()).isEqualTo(QUESTIONTIMEFRAME);
        assertThat(cs.getState().isQUESTIONTIMEFRAME());
        assertThat(cs.getQuestionN()).isEqualTo(1);

        TQ1 = currentTimeMillis();
        cs = impl.getCurrentStateFromTQuestion1(TQ1);
        assertThat(cs.getState()).isEqualTo(QUESTIONTIMEFRAME);
        assertThat(cs.getState().isQUESTIONTIMEFRAME());
        assertThat(cs.getQuestionN()).isEqualTo(1);

        for (int n = FIRST_QUESTION; n < p.getNbQuestions(); n++) {
            TQ1 = currentTimeMillis() - n * p.getQuestionTimeframeInMs() - (n - 1 >= 0 ? n - 1 : 0) * p.getSynchrotimeInMs();
            cs = impl.getCurrentStateFromTQuestion1(TQ1);
            assertThat(cs.getState()).isEqualTo(SYNCHROTIME);
            assertThat(cs.getQuestionN()).isEqualTo(n);

            TQ1 = currentTimeMillis() - n * p.getQuestionTimeframeInMs() - n * p.getSynchrotimeInMs();
            cs = impl.getCurrentStateFromTQuestion1(TQ1);
            assertThat(cs.getState()).isEqualTo(QUESTIONTIMEFRAME);
            assertThat(cs.getState().isQUESTIONTIMEFRAME());
            assertThat(cs.getQuestionN()).isEqualTo(n + 1);
        }

        TQ1 = currentTimeMillis() - p.getNbQuestions() * p.getQuestionTimeframeInMs() - (p.getNbQuestions() - 1) * p.getSynchrotimeInMs();
        cs = impl.getCurrentStateFromTQuestion1(TQ1);

        assertThat(cs.getState()).isEqualTo(SYNCHROTIME);
        assertThat(cs.getQuestionN()).isEqualTo(p.getNbQuestions());

        TQ1 = currentTimeMillis() - p.getNbQuestions() * p.getQuestionTimeframeInMs() - p.getNbQuestions() * p.getSynchrotimeInMs();
        cs = impl.getCurrentStateFromTQuestion1(TQ1);

        assertThat(cs.getState()).isEqualTo(RANKING);
        assertThat(cs.getQuestionN()).isEqualTo(0);
    }

    @Test
    public void testExpirationFromTQuestion1() {
        long TQ1 = currentTimeMillis() - 1234l;
        assertThat(TQ1 + 1 * p.getQuestionTimeframeInMs() + 0 * p.getSynchrotimeInMs()) //
                .isEqualTo(impl.getExpirationTimeForState(TQ1, State.QUESTIONTIMEFRAME, 1).getTime());
        assertThat(TQ1 + 1 * p.getQuestionTimeframeInMs() + 1 * p.getSynchrotimeInMs()) //
                .isEqualTo(impl.getExpirationTimeForState(TQ1, State.SYNCHROTIME, 1).getTime());
        assertThat(TQ1 + 2 * p.getQuestionTimeframeInMs() + 1 * p.getSynchrotimeInMs()) //
                .isEqualTo(impl.getExpirationTimeForState(TQ1, State.QUESTIONTIMEFRAME, 2).getTime());
        assertThat(TQ1 + 2 * p.getQuestionTimeframeInMs() + 2 * p.getSynchrotimeInMs()) //
                .isEqualTo(impl.getExpirationTimeForState(TQ1, State.SYNCHROTIME, 2).getTime());
    }

    @Test
    public void testOnGameStatusSwitchToLOGIN_timeout() throws Exception {
        gameStatusRepository.reset();
        GameStatus gameStatus = new GameStatus(p.getNbUsersThreshold() - 1, currentTimeMillis() - 10*p.getLoginTimeoutInMs(), currentTimeMillis() - (p.getLoginTimeoutInMs() / 2l), Long.MAX_VALUE, 0);
        usiPeriodicTaskTarget.onGameStatus(gameStatus);
        assertThat(usiMachine.getCurrentState().getState()).isEqualTo(LOGIN);
        assertThat(usiMachine.getCurrentState().getState().isLOGIN()).isTrue();
        sleep(1l + p.getLoginTimeoutInMs() / 2l);
        assertQUESTION1();
    }

    @Test
    public void testOnGameStatusSwitchToQUESTION1_and_SYNCHRO1_maxuser() throws Exception {
        gameStatusRepository.reset();
        GameStatus gameStatus = new GameStatus(p.getNbUsersThreshold(), currentTimeMillis() - 10*p.getLoginTimeoutInMs(), currentTimeMillis() - (p.getLoginTimeoutInMs() / 10l), Long.MAX_VALUE, 0);
        usiPeriodicTaskTarget.onGameStatus(gameStatus);
        assertQUESTION1();
        sleep(1 + p.getQuestionTimeframeInMs());
        assertSYNCHRO1();
    }

    @Test
    public void testOnGameStatusLateNeedToCatchUpToQUESTION1() throws Exception {
        gameStatusRepository.reset();
        // simulate a login status that just expired
        GameStatus gameStatus = new GameStatus(p.getNbUsersThreshold() - 1, currentTimeMillis() - 10*p.getLoginTimeoutInMs(), currentTimeMillis() - p.getLoginTimeoutInMs(), Long.MAX_VALUE, 0);
        usiPeriodicTaskTarget.onGameStatus(gameStatus);
        // the call above triggered a transition from start to login to question1
        assertQUESTION1();
    }

    @Test
    public void speedyGonzalesCatchUp() {
        gameStatusRepository.reset();
        GameStatus gameStatus = new GameStatus(p.getNbUsersThreshold() - 1, 0,//
                currentTimeMillis() - p.getLoginTimeoutInMs() - p.getNbQuestions() * (p.getQuestionTimeframeInMs() + p.getSynchrotimeInMs()), //
                currentTimeMillis() - p.getNbQuestions() * (p.getQuestionTimeframeInMs() + p.getSynchrotimeInMs()), 0);
        usiPeriodicTaskTarget.onGameStatus(gameStatus);
        assertRANKING();

        // now let's reset and do checks
        usiMachine.reset();
        assertThat(impl.initialized.get()).isFalse();
    }

    private void assertQUESTION1() {
        assertThat(usiMachine.getCurrentState().getState()).isEqualTo(QUESTIONTIMEFRAME);
        assertThat(usiMachine.getCurrentState().getState().isQUESTIONTIMEFRAME()).isTrue();
        assertThat(usiMachine.getCurrentState().getQuestionN()).isEqualTo(1);
    }

    private void assertSYNCHRO1() {
        assertThat(usiMachine.getCurrentState().getState()).isEqualTo(SYNCHROTIME);
        assertThat(usiMachine.getCurrentState().getState().isSYNCHROTIME()).isTrue();
        assertThat(usiMachine.getCurrentState().getQuestionN()).isEqualTo(1);
    }

    private void assertRANKING() {
        assertThat(usiMachine.getCurrentState().getState()).isEqualTo(RANKING);
        assertThat(usiMachine.getCurrentState().getState().isRANKING()).isTrue();
        assertThat(usiMachine.isRankingAllowed());
        assertThat(usiMachine.getCurrentState().getQuestionN()).isEqualTo(0);
    }
}