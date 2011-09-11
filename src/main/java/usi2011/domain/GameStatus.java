package usi2011.domain;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;

public class GameStatus {
    public static GameStatus DEFAULT = new GameStatus();

    long nbOfUsersLogged = 0;
    long gameCreationTime = 0;
    long whenFirstUserLoggedInMilliseconds = Long.MAX_VALUE;
    long question1TimeframeSTart = Long.MAX_VALUE;
    int nbTweetSent = 0;
    

    GameStatus() {
    }

    public GameStatus(long nbOfUsersLogged, long gameCreationTime, long whenFirstUserLoggedInMilliseconds, long question1TimeframeSTart, int nbTweetSent) {
        this.nbOfUsersLogged = nbOfUsersLogged;
        this.gameCreationTime = gameCreationTime;
        this.whenFirstUserLoggedInMilliseconds = whenFirstUserLoggedInMilliseconds;
        this.question1TimeframeSTart = question1TimeframeSTart;
        this.nbTweetSent = nbTweetSent;
    }

    public long getNbOfUsersLogged() {
        return nbOfUsersLogged;
    }

    public long getGameCreationTime() {
        return gameCreationTime;
    }

    public long getWhenFirstUserLoggedInMilliseconds() {
        return whenFirstUserLoggedInMilliseconds;
    }

    public boolean firstUserLoggedIn() {
        return whenFirstUserLoggedInMilliseconds != Long.MAX_VALUE;
    }

    public long getQuestion1TimeframeSTart() {
        return question1TimeframeSTart;
    }
    
    public int getNbTweetSent() {
        return nbTweetSent;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    public static class GameStatusBuilder extends GameStatus {

        public GameStatusBuilder setNbOfUsersLogged(long nbOfUsersLogged) {
            this.nbOfUsersLogged = nbOfUsersLogged;
            return this;
        }

        public GameStatusBuilder setGameCreationTime(long gameCreationTime) {
            this.gameCreationTime = gameCreationTime;
            return this;
        }

        public GameStatusBuilder setWhenFirstUserLoggedInMilliseconds(long whenFirstUserLoggedInMilliseconds) {
            this.whenFirstUserLoggedInMilliseconds = whenFirstUserLoggedInMilliseconds;
            return this;
        }

        public GameStatusBuilder setQuestion1TimeframeSTart(long question1TimeframeSTart) {
            this.question1TimeframeSTart = question1TimeframeSTart;
            return this;
        }

        public GameStatusBuilder setNbTweetSent(int nbTweetSent) {
            this.nbTweetSent = nbTweetSent;
            return this;
        }
        
        public GameStatus build() {
            return new GameStatus(nbOfUsersLogged, gameCreationTime, whenFirstUserLoggedInMilliseconds, question1TimeframeSTart, nbTweetSent);
        }
    }
}