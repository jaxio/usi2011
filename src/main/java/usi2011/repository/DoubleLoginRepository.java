package usi2011.repository;


public interface DoubleLoginRepository {

    void reset();

    boolean isUserLoggedIn(String email);

    void userLoggedIn(String email);
}
