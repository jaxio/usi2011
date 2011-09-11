package usi2011.task;

import usi2011.domain.GameStatus;

/**
 * Task Poller endpoint.
 */
public interface PeriodicTask {

    /**
     * @param gameStatus the latest global login status known.
     */
    void onGameStatus(GameStatus gameStatus);

    /**
     * Obtain the current local question1 counter value in order to publish it
     */
    int getQuestion1Counter();
}