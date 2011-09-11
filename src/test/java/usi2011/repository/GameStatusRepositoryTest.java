package usi2011.repository;

import static java.lang.System.currentTimeMillis;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;

import usi2011.domain.GameStatus;
import usi2011.task.GameStatusPollerTask;

@Ignore
public class GameStatusRepositoryTest {
    static GameStatusRepository repository = null;
    static GameStatusPollerTask poller = null; 

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(GameStatusRepository.class);
        poller = context.getBean(GameStatusPollerTask.class);
        poller.cancel();
    }

    @Test
    public void reset() {
        repository.reset();
        assertThat(repository.getGameStatus()).isNull();
    }

    @Test
    public void firstLogin() {
        repository.firstLogin(1l);

        GameStatus gameStatus = repository.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.getWhenFirstUserLoggedInMilliseconds()).isEqualTo(1);

        repository.firstLogin(2l);

        gameStatus = repository.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.getWhenFirstUserLoggedInMilliseconds()).isEqualTo(2);

        repository.reset();
        assertThat(repository.getGameStatus()).isNull();
    }

    @Test
    public void loggedUsers() {
        repository.loggedUsers(1);

        GameStatus gameStatus = repository.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.getNbOfUsersLogged()).isEqualTo(1);

        repository.loggedUsers(2);
        gameStatus = repository.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.getNbOfUsersLogged()).isEqualTo(2);

        repository.reset();
        assertThat(repository.getGameStatus()).isNull();
    }

    @Test
    public void question1TimeframeSTart() {
        repository.reset();
        long question1TimeframeSTart = currentTimeMillis();
        repository.question1TimeframeSTart(question1TimeframeSTart);

        GameStatus gameStatus = repository.getGameStatus();

        assertThat(gameStatus).isNotNull();
        assertThat(gameStatus.getQuestion1TimeframeSTart()).isEqualTo(question1TimeframeSTart);

        repository.reset();
        assertThat(repository.getGameStatus()).isNull();
    }
    
    @Test
    public void publishAndReadRankingCounter() {
        repository.reset();
        repository.publishLocalRankingRequestCount("machine1", 100l, 1);
        repository.publishLocalRankingRequestCount("machine2", 100l, 2);
        repository.publishLocalRankingRequestCount("machine3", 100l, 5);        
        assertThat(repository.getGlobalRankingRequestCount(100l)).isEqualTo(8);
    }
}