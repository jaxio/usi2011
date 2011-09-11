package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.ResourceUtil.asString;
import nu.xom.ParsingException;

import org.junit.Before;
import org.junit.Test;

import usi2011.exception.GameSessionException;

public class SessionTest {
    private Session gameSession;

    @Before
    public void init() {
        gameSession = new Session();
    }

    @Test
    public void validFile() throws ParsingException {
        gameSession.loadFromXML(asString("session/gamesession-20-questions.xml"));
        assertThat(gameSession.getQuestions()).hasSize(20);
    }

    @Test
    public void urlEncodedGameSession() throws ParsingException {
        gameSession.loadFromXML(asString("session/game.url.encoded"));
        assertThat(gameSession.getQuestions()).hasSize(20);
        assertThat(gameSession.getParameters().getNbQuestions()).isEqualTo(20);
    }

    @Test(expected = GameSessionException.class)
    public void invalidXml() throws ParsingException {
        gameSession.loadFromXML(asString("session/gamesession-invalid-xml.xml"));
    }

    @Test(expected = GameSessionException.class)
    public void tooMuchChoices() throws ParsingException {
        gameSession.loadFromXML(asString("session/gamesession-too-much-choices.xml"));
    }

    @Test(expected = GameSessionException.class)
    public void tooFewChoices() throws ParsingException {
        gameSession.loadFromXML(asString("session/gamesession-too-few-choices.xml"));
    }

}