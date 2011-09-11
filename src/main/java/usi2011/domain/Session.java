package usi2011.domain;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang.StringEscapeUtils.unescapeXml;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.FastIntegerParser.parseInt;
import static usi2011.util.ResourceUtil.asString;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;
import static usi2011.util.Specifications.PARAMETERS;

import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;

import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import usi2011.domain.Parameters.ParametersBuilder;
import usi2011.domain.Question.QuestionBuilder;
import usi2011.exception.GameSessionException;
import usi2011.util.JSONBackedObject;
import usi2011.util.Json;

public final class Session extends JSONBackedObject {
    static private final Logger logger = getLogger(Session.class);
    private final List<Question> questions = newArrayList();
    private Parameters parameters;

    Session() {
    }

    public Session(String json) {
        this(new Json(json));
    }

    public Session(Json json) {
        assertAuthenticationKey(json);
        loadFromXML(getString(json, PARAMETERS));
    }

    public Session(Resource resource) {
        loadFromXML(asString(resource));
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Parameters getParameters() {
        return parameters;
    }

    void loadFromXML(String document) {
        try {
            document = unescapeXml(document);
            Builder builder = new Builder();

            Document doc = builder.build(document, "http://www.usi.com");
            Element root = doc.getRootElement();
            Elements children = root.getChildElements();
            int size = children.size();
            boolean hasQuestions = false;
            boolean hasParameters = false;
            for (int i = 0; i < size; i++) {
                Element element = children.get(i);
                String name = element.getQualifiedName();
                if ("usi:questions".equals(name)) {
                    buildQuestions(element);
                    hasQuestions = true;
                } else if ("usi:parameters".equals(name)) {
                    parameters = buildParameters(element.getChildElements());
                    hasParameters = true;
                }
            }
            if (!hasQuestions) {
                throw new GameSessionException("Could not find questions");
            }
            if (!hasParameters) {
                throw new GameSessionException("Could not find parameters");
            }
        } catch (GameSessionException e) {
            logger.error("failed to load game session xml document", e);
            throw e;
        } catch (ParsingException e) {
            logger.error("parsing exception", e);
            throw new GameSessionException("Parsing exception", e);
        } catch (Exception e) {
            logger.error("failed to load game session xml document", e);
            throw new GameSessionException("IllegalStateException", e);
        }
    }

    private void buildQuestions(Element element) {
        Elements xmlQuestions = element.getChildElements();
        if (xmlQuestions.size() > MAX_NUMBER_OF_QUESTIONS) {
            throw new GameSessionException("More than the maximum number of questions : " + MAX_NUMBER_OF_QUESTIONS + " cf XSD");
        }
        for (int q = 0; q < xmlQuestions.size(); q++) {
            questions.add(buildQuestion(xmlQuestions.get(q), q + 1));
        }
    }

    private Parameters buildParameters(Elements parametersElement) {
        final int size = parametersElement.size();
        final ParametersBuilder builder = new ParametersBuilder();
        for (int q = 0; q < size; q++) {
            final Element element = parametersElement.get(q);
            final String name = element.getQualifiedName();
            final String value = trimToEmpty(element.getValue());
            if ("usi:logintimeout".equals(name)) {
                builder.setLoginTimeoutInSeconds(parseInt(value));
            } else if ("usi:synchrotime".equals(name)) {
                builder.setSynchrotimeInSeconds(parseInt(value));
            } else if ("usi:nbusersthreshold".equals(name)) {
                builder.setNbUsersThreshold(parseInt(value));
            } else if ("usi:questiontimeframe".equals(name)) {
                builder.setQuestionTimeframeInSeconds(parseInt(value));
            } else if ("usi:nbquestions".equals(name)) {
                builder.setNbQuestions(parseInt(value));
            } else if ("usi:flushusertable".equals(name)) {
                builder.setFlushUserTable(parseBoolean(value));
            }
        }
        return builder.build();
    }

    private Question buildQuestion(Element questionElement, int questionId) {
        final int goodChoice = parseInt(questionElement.getAttribute("goodchoice").getValue());
        final QuestionBuilder builder = new QuestionBuilder()//
                .setQuestionId(questionId)//
                .setGoodChoice(goodChoice);
        setLabelAndChoices(builder, questionElement);
        return builder.build();
    }

    private void setLabelAndChoices(QuestionBuilder builder, Element questionElement) {
        final Elements questionItems = questionElement.getChildElements();
        int choice = 0;
        for (int q = 0; q < questionItems.size(); q++) {
            final Element questionItem = questionItems.get(q);
            final String qualifiedName = questionItem.getQualifiedName();
            final String value = trimToEmpty(questionItem.getValue());
            if ("usi:label".equals(qualifiedName)) {
                builder.setLabel(value);
            } else if ("usi:choice".equals(qualifiedName)) {
                switch (choice) {
                case 0:
                    builder.setChoice1(value);
                    break;
                case 1:
                    builder.setChoice2(value);
                    break;
                case 2:
                    builder.setChoice3(value);
                    break;
                case 3:
                    builder.setChoice4(value);
                    break;
                default:
                    throw new GameSessionException("More than " + MAX_NUMBER_OF_ANSWERS + " choices for question " + questionItems.toString());
                }
                choice++;
            }
        }
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    @Override
    public RuntimeException buildException(String message) {
        return new GameSessionException(message);
    }
}