package usi2011.util;

import java.util.concurrent.ThreadFactory;

public final class NamedThreadFactory implements ThreadFactory {
    private final String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, name);
    }
}
