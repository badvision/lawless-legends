package jace;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom exception handler to suppress noisy MacAccessible errors in native images.
 *
 * MacAccessible requires native methods that aren't available in GraalVM native images,
 * causing benign NoClassDefFoundError exceptions that clutter the console. This handler
 * silently ignores those specific errors while still reporting other exceptions.
 */
public class MacAccessibleExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(MacAccessibleExceptionHandler.class.getName());
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public MacAccessibleExceptionHandler(Thread.UncaughtExceptionHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // Suppress MacAccessible-related errors (they're benign in native images)
        if (isMacAccessibleError(e)) {
            LOGGER.log(Level.FINE, "Suppressed MacAccessible error (benign in native image)", e);
            return;
        }

        // Pass all other exceptions to the default handler
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), e);
        }
    }

    private boolean isMacAccessibleError(Throwable e) {
        if (e == null) return false;

        String message = e.getMessage();
        String className = e.getClass().getName();

        // Check if it's a MacAccessible-related error
        if (className.equals("java.lang.NoClassDefFoundError") &&
            message != null && message.contains("MacAccessible")) {
            return true;
        }

        if (className.equals("java.lang.NoSuchMethodError") &&
            message != null && message.contains("MacAccessible")) {
            return true;
        }

        return false;
    }
}
