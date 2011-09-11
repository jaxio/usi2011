package usi2011.repository;

import usi2011.domain.User;

public interface LoginStatusRepository {
    public static enum LoginStatusMetadata {
        IS_LOGGED
    }

    void reset();

    boolean isLoggedIn(User user);

    void userLoggedIn(User user);
}
