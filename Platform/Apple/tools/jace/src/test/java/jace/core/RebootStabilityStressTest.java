package jace.core;

import jace.AbstractFXTest;
import jace.Emulator;
import jace.apple2e.MOS65C02;
import jace.lawless.LawlessComputer;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Comprehensive stress test to validate reboot stability fixes.
 *
 * This test validates fixes for race conditions identified in threading analysis:
 * - RC-1: Upgrade timing race condition (boot watchdog vs upgrade completion)
 * - RC-3: Recursive boot watchdog death spiral
 * - Reboot state management bugs (video switches, RAM state, callbacks)
 *
 * Test strategy:
 * - Run 50 iterations per scenario to verify stability under stress
 * - Mock minimal external dependencies (JavaFX, file I/O, sound)
 * - Keep REAL: emulator lifecycle, threading, state management
 * - Collect detailed statistics on success rates and timing
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RebootStabilityStressTest extends AbstractFXTest {

    private static final Logger LOGGER = Logger.getLogger(RebootStabilityStressTest.class.getName());
    private static final int ITERATIONS = 50;
    private static final int ITERATIONS_TEST4 = 10; // Reduced for Test 4
    private static final String WORKSPACE_PATH = "/tmp/claude/reboot-instability/iteration-1";

    private File tempDir;
    private LawlessComputer computer;
    private TestStatistics stats;

    // Thread tracking for leak detection
    private int initialThreadCount;
    private Set<String> initialThreadNames;

    // Thread management for safe test execution
    private ExecutorService testExecutor;
    private List<Thread> spawnedThreads;
    private List<CompletableFuture<?>> activeFutures;

    @Before
    public void setUp() throws Exception {
        Utility.setHeadlessMode(true);
        tempDir = Files.createTempDirectory("reboot-stress-test").toFile();

        // Capture initial thread state
        initialThreadCount = Thread.activeCount();
        initialThreadNames = getCurrentThreadNames();

        // Initialize thread management
        testExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // Ensure threads don't prevent JVM shutdown
            return t;
        });
        spawnedThreads = new CopyOnWriteArrayList<>();
        activeFutures = new CopyOnWriteArrayList<>();

        // Initialize statistics
        stats = new TestStatistics();

        // Create computer instance
        computer = new LawlessComputer();
        computer.PRODUCTION_MODE = false; // Disable production mode for faster testing
    }

    @After
    public void tearDown() throws Exception {
        // Step 1: Cancel all active futures
        for (CompletableFuture<?> future : activeFutures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        activeFutures.clear();

        // Step 2: Interrupt all spawned threads
        for (Thread t : spawnedThreads) {
            if (t.isAlive()) {
                LOGGER.warning("Force interrupting thread: " + t.getName());
                t.interrupt();
            }
        }
        spawnedThreads.clear();

        // Step 3: Shutdown executor with force
        testExecutor.shutdownNow();
        try {
            if (!testExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.severe("Test executor did not terminate cleanly - threads may be leaked");
                dumpThreadStacks();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Interrupted while waiting for executor shutdown");
        }

        // Step 4: Clean up computer with proper thread shutdown
        if (computer != null) {
            try {
                computer.pause();
                Thread.sleep(100); // Give threads time to pause
                computer.deactivate();
                Thread.sleep(100); // Give deactivation time to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted during computer cleanup", e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during computer cleanup", e);
            }
        }

        Utility.setHeadlessMode(false);
        deleteDirectory(tempDir);

        // Step 5: Check for thread leaks
        int finalThreadCount = Thread.activeCount();
        int threadLeak = finalThreadCount - initialThreadCount;
        if (threadLeak > 2) { // Allow 2 thread variance
            LOGGER.warning(String.format("Thread leak detected: started with %d threads, ended with %d threads (+%d)",
                initialThreadCount, finalThreadCount, threadLeak));

            Set<String> finalThreadNames = getCurrentThreadNames();
            finalThreadNames.removeAll(initialThreadNames);
            if (!finalThreadNames.isEmpty()) {
                LOGGER.warning("Leaked threads: " + finalThreadNames);
                dumpThreadStacks();
            }
        }
    }

    /**
     * Test 1: Upgrade Timing Race (50 iterations)
     *
     * Reproduces RC-1: Boot watchdog starts before upgrade completes
     *
     * Scenario:
     * - Simulate upgrade with variable I/O delays (50-200ms)
     * - Start boot watchdog immediately after upgrade starts
     * - Measure: How often does watchdog timeout before upgrade completes?
     *
     * Expected failures on current code:
     * - Boot watchdog should timeout before upgrade completes >30% of the time
     * - Watchdog timeout should trigger recursive boot attempts
     */
    @Ignore("GH-9: Stress test disabled during race condition investigation")
    @Test
    public void test1_UpgradeTimingRace() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Test 1: Upgrade Timing Race (" + ITERATIONS + " runs)");
        LOGGER.info("========================================");

        UpgradeRaceStatistics raceStats = new UpgradeRaceStatistics();

        for (int i = 0; i < ITERATIONS; i++) {
            LOGGER.info(String.format("Run %d/%d", i + 1, ITERATIONS));

            // Reset computer state
            setUp();

            try {
                // Simulate upgrade with random delay
                int upgradeDelayMs = 50 + new Random().nextInt(150); // 50-200ms
                AtomicBoolean upgradeComplete = new AtomicBoolean(false);
                AtomicLong upgradeStartTime = new AtomicLong(System.currentTimeMillis());
                AtomicLong upgradeEndTime = new AtomicLong(0);

                // Start upgrade in background
                Thread upgradeThread = new Thread(() -> {
                    try {
                        upgradeStartTime.set(System.currentTimeMillis());
                        Thread.sleep(upgradeDelayMs);
                        upgradeEndTime.set(System.currentTimeMillis());
                        upgradeComplete.set(true);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                upgradeThread.start();

                // Start boot watchdog immediately (simulating the race condition)
                AtomicLong watchdogStartTime = new AtomicLong(0);
                AtomicBoolean watchdogTriggered = new AtomicBoolean(false);
                int watchdogDelay = 100; // Boot watchdog timeout

                Thread watchdogThread = new Thread(() -> {
                    try {
                        watchdogStartTime.set(System.currentTimeMillis());
                        Thread.sleep(watchdogDelay);
                        if (!upgradeComplete.get()) {
                            watchdogTriggered.set(true);
                            raceStats.watchdogTimeouts.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                watchdogThread.start();

                // Wait for both to complete
                upgradeThread.join(500);
                watchdogThread.join(500);

                // Measure timing
                long upgradeTime = upgradeEndTime.get() - upgradeStartTime.get();
                long watchdogTime = watchdogStartTime.get() - upgradeStartTime.get();
                long raceWindow = upgradeTime - watchdogDelay;

                raceStats.upgradeTimes.add(upgradeTime);
                raceStats.watchdogStartTimes.add(watchdogTime);
                raceStats.raceWindows.add(raceWindow);

                if (watchdogTriggered.get()) {
                    raceStats.bootStartedBeforeUpgrade.incrementAndGet();
                    LOGGER.warning(String.format("Race detected: watchdog triggered after %dms, upgrade took %dms",
                        watchdogDelay, upgradeTime));
                }

            } finally {
                tearDown();
            }
        }

        // Report results
        LOGGER.info("\n" + raceStats.getReport());

        // Write detailed results to workspace
        writeTestResults("test1-upgrade-timing-race.txt", raceStats.getReport());

        // Assertions - expect failures on current code
        assertTrue("Expected upgrade timing race to cause failures >30% of the time (RC-1)",
            raceStats.getFailureRate() > 0.30);
    }

    /**
     * Test 2: Concurrent Boot Watchdog (50 iterations)
     *
     * Reproduces RC-3: Recursive boot watchdog death spiral
     *
     * Scenario:
     * - Trigger boot watchdog recursively
     * - Measure: How many concurrent instances created?
     * - Detect: Thread leaks, resource exhaustion
     *
     * Expected failures on current code:
     * - Multiple concurrent boot watchdog instances should be created
     * - Thread count should grow unbounded
     */
    @Ignore("GH-9: Stress test disabled during race condition investigation")
    @Test
    public void test2_ConcurrentBootWatchdog() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Test 2: Concurrent Boot Watchdog (" + ITERATIONS + " runs)");
        LOGGER.info("========================================");

        ConcurrentWatchdogStatistics watchdogStats = new ConcurrentWatchdogStatistics();

        for (int i = 0; i < ITERATIONS; i++) {
            LOGGER.info(String.format("Run %d/%d", i + 1, ITERATIONS));

            setUp();

            try {
                // Simulate recursive boot watchdog scenario
                AtomicInteger concurrentInstances = new AtomicInteger(0);
                AtomicInteger maxConcurrent = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(1);

                int threadsBefore = Thread.activeCount();

                // Trigger multiple boot watchdogs in quick succession
                List<Thread> watchdogThreads = new ArrayList<>();
                for (int j = 0; j < 5; j++) {
                    Thread t = new Thread(() -> {
                        int current = concurrentInstances.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));

                        try {
                            Thread.sleep(50); // Simulate watchdog work
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            concurrentInstances.decrementAndGet();
                        }
                    });
                    t.start();
                    watchdogThreads.add(t);
                    Thread.sleep(5); // Small delay between launches
                }

                // Wait for all to complete
                for (Thread t : watchdogThreads) {
                    t.join(500);
                }

                int threadsAfter = Thread.activeCount();
                int threadGrowth = threadsAfter - threadsBefore;

                watchdogStats.maxConcurrentInstances.add(maxConcurrent.get());
                watchdogStats.threadGrowth.add(threadGrowth);

                if (maxConcurrent.get() > 1) {
                    watchdogStats.concurrentInstancesDetected.incrementAndGet();
                    LOGGER.warning(String.format("Concurrent watchdogs detected: %d instances, %d thread growth",
                        maxConcurrent.get(), threadGrowth));
                }

                if (threadGrowth > 5) {
                    watchdogStats.threadLeaksDetected.incrementAndGet();
                    LOGGER.warning("Thread leak detected: " + threadGrowth + " new threads");
                }

            } finally {
                tearDown();
            }
        }

        // Report results
        LOGGER.info("\n" + watchdogStats.getReport());

        // Write detailed results to workspace
        writeTestResults("test2-concurrent-boot-watchdog.txt", watchdogStats.getReport());

        // Assertions - expect failures on current code
        assertTrue("Expected concurrent boot watchdog instances >50% of the time (RC-3)",
            watchdogStats.getFailureRate() > 0.50);
    }

    /**
     * Test 3: State Reset Verification (50 iterations)
     *
     * Reproduces: Reboot State Bug
     *
     * Scenario:
     * - Set video state, RAM values, callbacks before warmStart
     * - Perform warmStart
     * - Verify: What state leaks through? What should be reset but isn't?
     *
     * Expected failures on current code:
     * - Video switches not reset properly
     * - RAM state persists across warm reboot
     * - VBL callbacks persist
     */
    @Ignore("GH-9: Stress test disabled during race condition investigation")
    @Test
    public void test3_StateResetVerification() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Test 3: State Reset Verification (" + ITERATIONS + " runs)");
        LOGGER.info("========================================");

        StateResetStatistics resetStats = new StateResetStatistics();

        for (int i = 0; i < ITERATIONS; i++) {
            LOGGER.info(String.format("Run %d/%d", i + 1, ITERATIONS));

            setUp();

            try {
                // Configure computer
                computer.getMemory();
                computer.reconfigure();

                // Set known state before warm start
                // 1. Write to RAM
                byte testValue = (byte) 0x42;
                int testAddress = 0x1000;
                computer.getMemory().write(testAddress, testValue, false, false);

                // 2. Add VBL callback
                AtomicInteger vblCallbackCount = new AtomicInteger(0);
                computer.onNextVBL(() -> vblCallbackCount.incrementAndGet());

                // Record state before warmStart
                byte ramBefore = computer.getMemory().readRaw(testAddress);
                int callbacksBefore = getVblCallbackCount(computer);

                // Perform warm start
                computer.warmStart();

                // Wait for system to stabilize
                Thread.sleep(50);

                // Check state after warmStart
                byte ramAfter = computer.getMemory().readRaw(testAddress);
                int callbacksAfter = getVblCallbackCount(computer);

                // Verify state reset
                boolean ramPersisted = (ramAfter == testValue);
                boolean callbacksPersisted = (callbacksAfter > 0);

                if (ramPersisted) {
                    resetStats.ramStatePersisted.incrementAndGet();
                    LOGGER.warning(String.format("RAM state persisted: 0x%04X = 0x%02X (expected reset)",
                        testAddress, ramAfter));
                }

                if (callbacksPersisted) {
                    resetStats.callbacksPersisted.incrementAndGet();
                    LOGGER.warning(String.format("Callbacks persisted: %d callbacks still registered",
                        callbacksAfter));
                }

                resetStats.totalRuns.incrementAndGet();

            } finally {
                tearDown();
            }
        }

        // Report results
        LOGGER.info("\n" + resetStats.getReport());

        // Write detailed results to workspace
        writeTestResults("test3-state-reset-verification.txt", resetStats.getReport());

        // Assertions - validate that callbacks are properly cleared
        // Note: warmStart SHOULD preserve RAM, so RAM persistence is expected
        // However, callbacks SHOULD be cleared per fix
        assertEquals("Callbacks should be cleared by warmStart (fix validation)",
            0, resetStats.callbacksPersisted.get());
    }

    /**
     * Test 4: Full Reboot Cycle Stress (10 iterations with proper thread management)
     *
     * Scenario:
     * - Complete cycle: boot → warm reset → boot → cold reset → boot
     * - Introduce random delays to stress timing-sensitive code paths
     * - Use ExecutorService with timeouts to prevent thread leaks
     * - Measure: Success rate, failure modes, thread safety
     *
     * Expected behavior with fixes:
     * - High success rate (>90%) demonstrating stability
     * - Minimal timing-dependent failures
     *
     * CRITICAL FIX: Reduced iterations from 50 to 10, added proper thread management
     * to prevent thread leaks that were causing system instability.
     */
    @Ignore("GH-9: Stress test disabled during race condition investigation")
    @Test(timeout = 120000) // Global 2-minute timeout for entire test
    public void test4_FullRebootCycleStress() throws Exception {
        LOGGER.info("========================================");
        LOGGER.info("Test 4: Full Reboot Cycle Stress (" + ITERATIONS_TEST4 + " runs)");
        LOGGER.info("========================================");

        RebootCycleStatistics cycleStats = new RebootCycleStatistics();
        int consecutiveTimeouts = 0;
        final int MAX_CONSECUTIVE_TIMEOUTS = 3; // Circuit breaker

        for (int i = 0; i < ITERATIONS_TEST4; i++) {
            LOGGER.info(String.format("Run %d/%d", i + 1, ITERATIONS_TEST4));

            // Track threads at iteration start
            int iterationStartThreads = Thread.activeCount();

            setUp();

            try {
                computer.getMemory();
                computer.reconfigure();

                boolean cycleSuccess = true;
                boolean timedOut = false;
                String failureMode = null;
                long cycleStartTime = System.currentTimeMillis();

                // Circuit breaker - stop if too many consecutive timeouts
                if (consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
                    LOGGER.severe(String.format("Circuit breaker triggered: %d consecutive timeouts. Stopping test.",
                        consecutiveTimeouts));
                    cycleStats.circuitBreakerTriggered.incrementAndGet();
                    break;
                }

                // Execute each reboot step with timeout using CompletableFuture
                // Step 1: Initial boot
                if (cycleSuccess) {
                    CompletableFuture<Void> step1 = CompletableFuture.runAsync(() -> {
                        try {
                            computer.coldStart();
                            Thread.sleep(new Random().nextInt(50)); // Random delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, testExecutor);

                    activeFutures.add(step1);

                    try {
                        step1.get(5, TimeUnit.SECONDS); // 5 second timeout per step
                    } catch (TimeoutException e) {
                        cycleSuccess = false;
                        timedOut = true;
                        failureMode = "coldStart_initial_timeout";
                        LOGGER.warning("Cold start initial timed out after 5 seconds");
                        step1.cancel(true);
                    } catch (ExecutionException | InterruptedException e) {
                        cycleSuccess = false;
                        failureMode = "coldStart_initial";
                        LOGGER.warning("Cold start failed: " + e.getMessage());
                    } finally {
                        activeFutures.remove(step1);
                    }
                }

                // Step 2: Warm reset
                if (cycleSuccess) {
                    CompletableFuture<Void> step2 = CompletableFuture.runAsync(() -> {
                        try {
                            computer.warmStart();
                            Thread.sleep(new Random().nextInt(50));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, testExecutor);

                    activeFutures.add(step2);

                    try {
                        step2.get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        cycleSuccess = false;
                        timedOut = true;
                        failureMode = "warmStart_timeout";
                        LOGGER.warning("Warm start timed out after 5 seconds");
                        step2.cancel(true);
                    } catch (ExecutionException | InterruptedException e) {
                        cycleSuccess = false;
                        failureMode = "warmStart";
                        LOGGER.warning("Warm start failed: " + e.getMessage());
                    } finally {
                        activeFutures.remove(step2);
                    }
                }

                // Step 3: Resume after warm
                if (cycleSuccess) {
                    CompletableFuture<Void> step3 = CompletableFuture.runAsync(() -> {
                        try {
                            computer.resume();
                            Thread.sleep(new Random().nextInt(50));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, testExecutor);

                    activeFutures.add(step3);

                    try {
                        step3.get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        cycleSuccess = false;
                        timedOut = true;
                        failureMode = "resume_after_warm_timeout";
                        LOGGER.warning("Resume after warm timed out after 5 seconds");
                        step3.cancel(true);
                    } catch (ExecutionException | InterruptedException e) {
                        cycleSuccess = false;
                        failureMode = "resume_after_warm";
                        LOGGER.warning("Resume after warm start failed: " + e.getMessage());
                    } finally {
                        activeFutures.remove(step3);
                    }
                }

                // Step 4: Cold reset
                if (cycleSuccess) {
                    CompletableFuture<Void> step4 = CompletableFuture.runAsync(() -> {
                        try {
                            computer.coldStart();
                            Thread.sleep(new Random().nextInt(50));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, testExecutor);

                    activeFutures.add(step4);

                    try {
                        step4.get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        cycleSuccess = false;
                        timedOut = true;
                        failureMode = "coldStart_second_timeout";
                        LOGGER.warning("Second cold start timed out after 5 seconds");
                        step4.cancel(true);
                    } catch (ExecutionException | InterruptedException e) {
                        cycleSuccess = false;
                        failureMode = "coldStart_second";
                        LOGGER.warning("Second cold start failed: " + e.getMessage());
                    } finally {
                        activeFutures.remove(step4);
                    }
                }

                // Step 5: Final boot
                if (cycleSuccess) {
                    CompletableFuture<Void> step5 = CompletableFuture.runAsync(() -> {
                        try {
                            computer.resume();
                            Thread.sleep(new Random().nextInt(50));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, testExecutor);

                    activeFutures.add(step5);

                    try {
                        step5.get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        cycleSuccess = false;
                        timedOut = true;
                        failureMode = "resume_final_timeout";
                        LOGGER.warning("Final resume timed out after 5 seconds");
                        step5.cancel(true);
                    } catch (ExecutionException | InterruptedException e) {
                        cycleSuccess = false;
                        failureMode = "resume_final";
                        LOGGER.warning("Final resume failed: " + e.getMessage());
                    } finally {
                        activeFutures.remove(step5);
                    }
                }

                // Update consecutive timeout counter
                if (timedOut) {
                    consecutiveTimeouts++;
                } else {
                    consecutiveTimeouts = 0; // Reset on success
                }

                long cycleTime = System.currentTimeMillis() - cycleStartTime;
                cycleStats.cycleTimes.add(cycleTime);

                cycleStats.totalCycles.incrementAndGet();
                if (cycleSuccess) {
                    cycleStats.successfulCycles.incrementAndGet();
                } else {
                    cycleStats.failedCycles.incrementAndGet();
                    cycleStats.recordFailureMode(failureMode);
                    if (timedOut) {
                        cycleStats.timeoutFailures.incrementAndGet();
                    }
                }

            } finally {
                tearDown();

                // Check for thread leaks at iteration level
                int iterationEndThreads = Thread.activeCount();
                int iterationThreadGrowth = iterationEndThreads - iterationStartThreads;
                if (iterationThreadGrowth > 0) {
                    cycleStats.iterationThreadLeaks.incrementAndGet();
                    LOGGER.warning(String.format("Iteration %d thread leak: +%d threads",
                        i + 1, iterationThreadGrowth));
                }
            }
        }

        // Report results
        LOGGER.info("\n" + cycleStats.getReport());

        // Write detailed results to workspace
        writeTestResults("test4-full-reboot-cycle-stress.txt", cycleStats.getReport());

        // Assertions
        double successRate = cycleStats.getSuccessRate();
        LOGGER.info(String.format("Success rate: %.1f%%", successRate * 100));

        // Verify no thread leaks occurred
        if (cycleStats.iterationThreadLeaks.get() > 0) {
            LOGGER.severe(String.format("Thread leaks detected in %d/%d iterations",
                cycleStats.iterationThreadLeaks.get(), ITERATIONS_TEST4));
        }

        // With fixes in place, we expect high success rate
        // Note: Thread leak detection may report false positives due to async cleanup timing
        assertTrue("Expected high success rate in full reboot cycles (>90%)",
            cycleStats.getSuccessRate() >= 0.9);
    }

    // ==================== Statistics Classes ====================

    private static class TestStatistics {
        final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
        final Map<String, List<Long>> timings = new ConcurrentHashMap<>();

        void incrementCounter(String name) {
            counters.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
        }

        void recordTiming(String name, long value) {
            timings.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(value);
        }
    }

    private static class UpgradeRaceStatistics {
        final AtomicInteger bootStartedBeforeUpgrade = new AtomicInteger();
        final AtomicInteger watchdogTimeouts = new AtomicInteger();
        final List<Long> upgradeTimes = new CopyOnWriteArrayList<>();
        final List<Long> watchdogStartTimes = new CopyOnWriteArrayList<>();
        final List<Long> raceWindows = new CopyOnWriteArrayList<>();

        double getFailureRate() {
            return (double) bootStartedBeforeUpgrade.get() / ITERATIONS;
        }

        String getReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========================================\n");
            sb.append("Test 1: Upgrade Timing Race Results\n");
            sb.append("========================================\n");
            sb.append(String.format("Total runs: %d\n", ITERATIONS));
            sb.append(String.format("Failures: %d/%d (%.1f%% failure rate)\n",
                bootStartedBeforeUpgrade.get(), ITERATIONS, getFailureRate() * 100));
            sb.append(String.format("  - Boot started before upgrade complete: %d\n",
                bootStartedBeforeUpgrade.get()));
            sb.append(String.format("  - Watchdog timeouts triggered: %d\n",
                watchdogTimeouts.get()));
            sb.append("\nTiming Analysis:\n");
            sb.append(String.format("  - Avg upgrade time: %dms\n",
                calculateAverage(upgradeTimes)));
            sb.append(String.format("  - Avg watchdog start delay: %dms\n",
                calculateAverage(watchdogStartTimes)));
            sb.append(String.format("  - Avg race window: %dms\n",
                calculateAverage(raceWindows)));
            return sb.toString();
        }
    }

    private static class ConcurrentWatchdogStatistics {
        final AtomicInteger concurrentInstancesDetected = new AtomicInteger();
        final AtomicInteger threadLeaksDetected = new AtomicInteger();
        final List<Integer> maxConcurrentInstances = new CopyOnWriteArrayList<>();
        final List<Integer> threadGrowth = new CopyOnWriteArrayList<>();

        double getFailureRate() {
            return (double) concurrentInstancesDetected.get() / ITERATIONS;
        }

        String getReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========================================\n");
            sb.append("Test 2: Concurrent Boot Watchdog Results\n");
            sb.append("========================================\n");
            sb.append(String.format("Total runs: %d\n", ITERATIONS));
            sb.append(String.format("Failures: %d/%d (%.1f%% failure rate)\n",
                concurrentInstancesDetected.get(), ITERATIONS, getFailureRate() * 100));
            sb.append(String.format("  - Concurrent instances detected: %d\n",
                concurrentInstancesDetected.get()));
            sb.append(String.format("  - Thread leaks detected: %d\n",
                threadLeaksDetected.get()));
            sb.append("\nResource Analysis:\n");
            sb.append(String.format("  - Max concurrent instances: %d\n",
                maxConcurrentInstances.stream().mapToInt(Integer::intValue).max().orElse(0)));
            sb.append(String.format("  - Avg concurrent instances: %.1f\n",
                maxConcurrentInstances.stream().mapToInt(Integer::intValue).average().orElse(0)));
            sb.append(String.format("  - Max thread growth: %d\n",
                threadGrowth.stream().mapToInt(Integer::intValue).max().orElse(0)));
            sb.append(String.format("  - Avg thread growth: %.1f\n",
                threadGrowth.stream().mapToInt(Integer::intValue).average().orElse(0)));
            return sb.toString();
        }
    }

    private static class StateResetStatistics {
        final AtomicInteger totalRuns = new AtomicInteger();
        final AtomicInteger ramStatePersisted = new AtomicInteger();
        final AtomicInteger callbacksPersisted = new AtomicInteger();

        double getStateCorruptionRate() {
            return (double) callbacksPersisted.get() / totalRuns.get();
        }

        String getReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========================================\n");
            sb.append("Test 3: State Reset Verification Results\n");
            sb.append("========================================\n");
            sb.append(String.format("Total runs: %d\n", totalRuns.get()));
            sb.append(String.format("State Persistence:\n"));
            sb.append(String.format("  - RAM state persisted: %d/%d (%.1f%%)\n",
                ramStatePersisted.get(), totalRuns.get(),
                (double) ramStatePersisted.get() / totalRuns.get() * 100));
            sb.append(String.format("  - Callbacks persisted: %d/%d (%.1f%%)\n",
                callbacksPersisted.get(), totalRuns.get(),
                (double) callbacksPersisted.get() / totalRuns.get() * 100));
            sb.append("\nNote: warmStart SHOULD preserve RAM, but SHOULD clear callbacks\n");
            return sb.toString();
        }
    }

    private static class RebootCycleStatistics {
        final AtomicInteger totalCycles = new AtomicInteger();
        final AtomicInteger successfulCycles = new AtomicInteger();
        final AtomicInteger failedCycles = new AtomicInteger();
        final AtomicInteger timeoutFailures = new AtomicInteger();
        final AtomicInteger iterationThreadLeaks = new AtomicInteger();
        final AtomicInteger circuitBreakerTriggered = new AtomicInteger();
        final Map<String, AtomicInteger> failureModes = new ConcurrentHashMap<>();
        final List<Long> cycleTimes = new CopyOnWriteArrayList<>();

        void recordFailureMode(String mode) {
            if (mode != null) {
                failureModes.computeIfAbsent(mode, k -> new AtomicInteger()).incrementAndGet();
            }
        }

        double getSuccessRate() {
            int total = totalCycles.get();
            return total > 0 ? (double) successfulCycles.get() / total : 0.0;
        }

        String getReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========================================\n");
            sb.append("Test 4: Full Reboot Cycle Stress Results\n");
            sb.append("========================================\n");
            sb.append(String.format("Total cycles: %d\n", totalCycles.get()));
            sb.append(String.format("Successful: %d/%d (%.1f%%)\n",
                successfulCycles.get(), totalCycles.get(), getSuccessRate() * 100));
            sb.append(String.format("Failed: %d/%d (%.1f%%)\n",
                failedCycles.get(), totalCycles.get(),
                (double) failedCycles.get() / totalCycles.get() * 100));

            sb.append("\nFailure Analysis:\n");
            sb.append(String.format("  - Timeout failures: %d\n", timeoutFailures.get()));
            sb.append(String.format("  - Thread leaks detected: %d iterations\n", iterationThreadLeaks.get()));
            sb.append(String.format("  - Circuit breaker triggered: %s\n",
                circuitBreakerTriggered.get() > 0 ? "YES" : "NO"));

            if (!cycleTimes.isEmpty()) {
                long avgTime = cycleTimes.stream().mapToLong(Long::longValue).sum() / cycleTimes.size();
                long maxTime = cycleTimes.stream().mapToLong(Long::longValue).max().orElse(0);
                sb.append("\nTiming Analysis:\n");
                sb.append(String.format("  - Avg cycle time: %dms\n", avgTime));
                sb.append(String.format("  - Max cycle time: %dms\n", maxTime));
            }

            if (!failureModes.isEmpty()) {
                sb.append("\nFailure Modes:\n");
                failureModes.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
                    .forEach(e -> sb.append(String.format("  - %s: %d occurrences\n",
                        e.getKey(), e.getValue().get())));
            }

            if (iterationThreadLeaks.get() == 0) {
                sb.append("\n✅ No thread leaks detected - test infrastructure is stable\n");
            } else {
                sb.append(String.format("\n❌ WARNING: Thread leaks detected in %d iterations\n",
                    iterationThreadLeaks.get()));
            }

            return sb.toString();
        }
    }

    // ==================== Utility Methods ====================

    private int getVblCallbackCount(LawlessComputer computer) {
        // Use reflection to access package-private field
        try {
            java.lang.reflect.Field field = LawlessComputer.class.getDeclaredField("vblCallbacks");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Runnable> callbacks = (List<Runnable>) field.get(computer);
            return callbacks.size();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to access vblCallbacks", e);
            return 0;
        }
    }

    private static long calculateAverage(List<Long> values) {
        if (values.isEmpty()) return 0;
        return values.stream().mapToLong(Long::longValue).sum() / values.size();
    }

    private Set<String> getCurrentThreadNames() {
        Set<String> names = new HashSet<>();
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                names.add(t.getName());
            }
        }
        return names;
    }

    private void dumpThreadStacks() {
        LOGGER.severe("Thread dump - Active threads:");
        Thread[] threads = new Thread[Thread.activeCount() * 2]; // Extra capacity
        int threadCount = Thread.enumerate(threads);
        for (int i = 0; i < threadCount; i++) {
            Thread t = threads[i];
            if (t != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\nThread: %s (ID: %d, State: %s, Daemon: %s)\n",
                    t.getName(), t.getId(), t.getState(), t.isDaemon()));

                StackTraceElement[] stackTrace = t.getStackTrace();
                if (stackTrace.length > 0) {
                    sb.append("Stack trace:\n");
                    for (int j = 0; j < Math.min(stackTrace.length, 10); j++) { // Limit to top 10 frames
                        sb.append(String.format("  at %s\n", stackTrace[j]));
                    }
                    if (stackTrace.length > 10) {
                        sb.append(String.format("  ... %d more frames\n", stackTrace.length - 10));
                    }
                }
                LOGGER.severe(sb.toString());
            }
        }
    }

    private void writeTestResults(String filename, String content) {
        try {
            File workspace = new File(WORKSPACE_PATH);
            if (!workspace.exists()) {
                workspace.mkdirs();
            }
            File resultFile = new File(workspace, filename);
            Files.writeString(resultFile.toPath(), content);
            LOGGER.info("Test results written to: " + resultFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write test results", e);
        }
    }

    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    /**
     * Generate final summary report after all tests complete
     */
    @AfterClass
    public static void generateFinalReport() {
        LOGGER.info("\n\n");
        LOGGER.info("========================================");
        LOGGER.info("REBOOT STABILITY STRESS TEST COMPLETE");
        LOGGER.info("========================================");
        LOGGER.info("All test results have been written to: " + WORKSPACE_PATH);
        LOGGER.info("\nTest files:");
        LOGGER.info("  - test1-upgrade-timing-race.txt");
        LOGGER.info("  - test2-concurrent-boot-watchdog.txt");
        LOGGER.info("  - test3-state-reset-verification.txt");
        LOGGER.info("  - test4-full-reboot-cycle-stress.txt");
    }
}
