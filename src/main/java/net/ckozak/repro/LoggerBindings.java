package net.ckozak.repro;

import java.util.concurrent.atomic.AtomicBoolean;

public final class LoggerBindings {

    private static final AtomicBoolean initialized = new AtomicBoolean();

    public static void initialize() {
        if (initialized.compareAndSet(false, true)) {
            System.setProperty("org.jboss.logging.provider", "log4j2");
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        }
    }

    private LoggerBindings() { /* utility class */ }
}
