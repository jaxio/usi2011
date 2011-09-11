package usi2011.statemachine.support;

public interface StateCallback {
    void success();

    void failure(String reason);
}