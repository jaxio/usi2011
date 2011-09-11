package usi2011.repository;

import usi2011.domain.User;

public interface UserRepository {
    public static enum UsersMetadata {
        FIRSTNAME_JSON_QUOTED, //
        LASTNAME_JSON_QUOTED, //
        PASSWORD_JSON_QUOTED, //
        EMAIL_JSON_QUOTED, //
        FIRSTNAME, //
        LASTNAME, //
        PASSWORD, //
        LOGGED_IN_GAME;
    }

    void reset();

    boolean save(User user);

    User get(String email);

    void userLoggedIn(User user, long gameId);
    
    public void resetCacheHitStats();
    public void clearAllUsersFromMemory();
    public void loadUsersInMemory();    
}
