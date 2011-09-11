package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import usi2011.domain.GameStatus.GameStatusBuilder;

public class GameStatusTest {
    @Test
    public void build() {
        GameStatus gameStatus = gameStatusBuilder().build();

        assertThat(gameStatus.getNbOfUsersLogged()).isEqualTo(1);
        assertThat(gameStatus.getWhenFirstUserLoggedInMilliseconds()).isEqualTo(2);
    }

    @Test
    public void emptyGameStatusIsValid() {
        GameStatus gameStatus = new GameStatusBuilder().build();
        assertThat(gameStatus.firstUserLoggedIn()).isFalse();
        assertThat(gameStatus.getNbOfUsersLogged()).isEqualTo(0);
        assertThat(gameStatus.getWhenFirstUserLoggedInMilliseconds()).isEqualTo(Long.MAX_VALUE);
    }

    private GameStatusBuilder gameStatusBuilder() {
        return new GameStatusBuilder() //
                .setNbOfUsersLogged(1) //
                .setWhenFirstUserLoggedInMilliseconds(2);
    }
}
