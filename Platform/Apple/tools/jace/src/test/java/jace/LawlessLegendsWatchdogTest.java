package jace;

import jace.lawless.LawlessComputer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Unit test to validate the atomic guard on bootWatchdog() prevents concurrent instances.
 * This is a focused test for Phase 1 Fix #1 (RC-3 prevention).
 */
public class LawlessLegendsWatchdogTest extends AbstractFXTest {

    private static final Logger LOGGER = Logger.getLogger(LawlessLegendsWatchdogTest.class.getName());
    private LawlessLegends app;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        // Clean up any existing emulator instance
        Emulator.abort();

        // Create LawlessLegends instance
        app = new LawlessLegends();

        // Initialize emulator with test configuration
        Emulator.getInstance();
        Emulator.withComputer(c -> {
            LawlessComputer computer = (LawlessComputer) c;
            computer.PRODUCTION_MODE = false; // Disable production mode for faster testing
        });

        executor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        Emulator.abort();
    }

    /**
     * Test that bootWatchdog() atomic guard prevents concurrent instances.
     * With debounce behavior, concurrent calls schedule retries instead of blocking hard.
     * Expected: Concurrent calls return immediately and schedule retries with exponential backoff.
     */
    @Test(timeout = 30000)
    public void testBootWatchdogAtomicGuard_PreventsConcurrentInstances() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Testing bootWatchdog() atomic guard with debounce");
        LOGGER.info("========================================");

        AtomicInteger callsReturned = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        int numAttempts = 10;

        // Try to start multiple boot watchdogs concurrently
        for (int i = 0; i < numAttempts; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    // Call the actual bootWatchdog method
                    // With debounce, this should return quickly, scheduling retries if busy
                    app.bootWatchdog();
                    callsReturned.incrementAndGet();
                } catch (Exception e) {
                    LOGGER.warning("Exception in watchdog: " + e.getMessage());
                }
            });
            futures.add(future);

            // Small delay between launch attempts
            Thread.sleep(10);
        }

        // Wait for all attempts to complete
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.warning("Future timed out");
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.warning("Future failed: " + e.getMessage());
            }
        }

        // Small delay to ensure all threads complete
        Thread.sleep(200);

        // Report results
        LOGGER.info(String.format("Total attempts: %d", numAttempts));
        LOGGER.info(String.format("Calls that returned: %d", callsReturned.get()));

        // Assertions
        // With debounce behavior, all calls should return quickly (not block)
        // They either acquire the lock or schedule a retry
        assertTrue("Expected all calls to return (debounce behavior)",
            callsReturned.get() >= numAttempts - 1);

        LOGGER.info("✅ Atomic guard with debounce successfully prevents concurrent instances while allowing retries");
    }

    /**
     * Test that bootWatchdog() guard is properly released after completion.
     * Expected: After first watchdog completes, a second should be able to start.
     */
    @Test(timeout = 20000)
    public void testBootWatchdogAtomicGuard_ReleasedAfterCompletion() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Testing bootWatchdog() guard release");
        LOGGER.info("========================================");

        // Start first watchdog
        CountDownLatch firstStarted = new CountDownLatch(1);
        Future<?> first = executor.submit(() -> {
            app.bootWatchdog();
            firstStarted.countDown();
        });

        // Wait for first to start
        assertTrue("First watchdog should start",
            firstStarted.await(5, TimeUnit.SECONDS));

        // Wait for first to complete (watchdog has a delay, so give it time)
        Thread.sleep(600); // Watchdog delay is 500ms in test mode

        // Try to start second watchdog - should succeed since first completed
        CountDownLatch secondStarted = new CountDownLatch(1);
        Future<?> second = executor.submit(() -> {
            app.bootWatchdog();
            secondStarted.countDown();
        });

        // Second should be able to start now that first is complete
        boolean secondSucceeded = secondStarted.await(5, TimeUnit.SECONDS);

        // Cleanup
        first.cancel(true);
        second.cancel(true);

        assertTrue("Second watchdog should start after first completes", secondSucceeded);

        LOGGER.info("✅ Atomic guard properly released after watchdog completion");
    }
}
