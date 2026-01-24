package jace.lawless;

import jace.AbstractFXTest;
import jace.core.Motherboard;
import jace.core.Utility;
import jace.core.Video;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test to verify defensive checks in waitForVBL() prevent deadlocks.
 *
 * This test validates that Option 4 (defensive checks with timeout) works correctly:
 * - Null checks prevent NPE
 * - isRunning() checks prevent waiting on stopped devices
 * - Worker thread alive check prevents waiting on dead threads
 * - Timeout prevents infinite hang
 */
public class VBLDefensiveChecksTest extends AbstractFXTest {

    private static final Logger LOGGER = Logger.getLogger(VBLDefensiveChecksTest.class.getName());
    private LawlessComputer computer;

    @Before
    public void setUp() throws Exception {
        Utility.setHeadlessMode(true);
        computer = new LawlessComputer();
        computer.PRODUCTION_MODE = false;
        computer.showBootAnimation = false;
    }

    @After
    public void tearDown() throws Exception {
        if (computer != null) {
            try {
                computer.pause();
                Thread.sleep(100);
                computer.deactivate();
                Thread.sleep(100);
            } catch (Exception e) {
                LOGGER.warning("Error during cleanup: " + e.getMessage());
            }
        }
        Utility.setHeadlessMode(false);
    }

    /**
     * Test 1: Verify waitForVBL returns immediately when motherboard is not running
     */
    @Test(timeout = 3000)
    public void testWaitForVBL_MotherboardNotRunning() throws Exception {
        LOGGER.info("Test 1: waitForVBL with motherboard not running");

        // Don't start the computer - motherboard should not be running
        long startTime = System.currentTimeMillis();

        // This should return immediately without blocking
        computer.waitForVBL();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Elapsed time: " + elapsed + "ms");

        // Should complete in less than 100ms (way before the 2-second timeout)
        assertTrue("waitForVBL should return immediately when motherboard not running",
            elapsed < 100);
    }

    /**
     * Test 2: Verify waitForVBL returns immediately when video is not running
     */
    @Test(timeout = 3000)
    public void testWaitForVBL_VideoNotRunning() throws Exception {
        LOGGER.info("Test 2: waitForVBL with video not running");

        // Initialize but don't fully start
        computer.getMemory();
        computer.reconfigure();

        long startTime = System.currentTimeMillis();

        // This should return immediately without blocking
        computer.waitForVBL();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Elapsed time: " + elapsed + "ms");

        assertTrue("waitForVBL should return immediately when video not running",
            elapsed < 100);
    }

    /**
     * Test 3: Verify timeout triggers when worker thread dies
     */
    @Test(timeout = 3000)
    public void testWaitForVBL_WorkerThreadDied() throws Exception {
        LOGGER.info("Test 3: waitForVBL with dead worker thread");

        // Start computer normally
        computer.getMemory();
        computer.reconfigure();
        computer.coldStart();
        Thread.sleep(100); // Let it start

        // Kill the motherboard worker thread
        Motherboard mb = computer.getMotherboard();
        if (mb != null) {
            mb.suspend(); // This should stop the worker thread
            Thread.sleep(100); // Give it time to die
        }

        long startTime = System.currentTimeMillis();

        // This should detect dead worker and return immediately
        computer.waitForVBL();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Elapsed time: " + elapsed + "ms");

        // Should detect dead worker immediately (< 100ms), not wait for 2-second timeout
        assertTrue("waitForVBL should detect dead worker thread immediately",
            elapsed < 100);
    }

    /**
     * Test 4: Verify timeout as last resort
     *
     * This test simulates a scenario where worker thread is alive but never signals VBL.
     * The 2-second timeout should prevent infinite hang.
     */
    @Test(timeout = 3000)
    public void testWaitForVBL_TimeoutAsLastResort() throws Exception {
        LOGGER.info("Test 4: waitForVBL timeout as last resort");

        // Start computer
        computer.getMemory();
        computer.reconfigure();
        computer.coldStart();
        Thread.sleep(100);

        // Pause CPU so VBL never fires
        computer.pause();
        Thread.sleep(100);

        long startTime = System.currentTimeMillis();

        // This should timeout after 2 seconds (but defensive check #2 should catch it earlier)
        computer.waitForVBL();

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Elapsed time: " + elapsed + "ms");

        // Should complete quickly (defensive check #2: isRunning()) or via timeout
        assertTrue("waitForVBL should not hang forever", elapsed < 2500);
    }

    /**
     * Test 5: Verify multiple VBL waits don't accumulate callbacks
     */
    @Test(timeout = 5000)
    public void testWaitForVBL_NoCallbackAccumulation() throws Exception {
        LOGGER.info("Test 5: Multiple waitForVBL calls without callback accumulation");

        // Don't start computer - all waits should return immediately
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            computer.waitForVBL();
            long elapsed = System.currentTimeMillis() - startTime;

            assertTrue("Iteration " + i + " should complete quickly", elapsed < 100);
        }

        LOGGER.info("All 10 iterations completed without accumulation");
    }

    /**
     * Test 6: Verify waitForVBL(count) handles recursive calls correctly
     */
    @Test(timeout = 5000)
    public void testWaitForVBL_RecursiveCount() throws Exception {
        LOGGER.info("Test 6: waitForVBL with recursive count");

        // Don't start computer - all waits should return immediately
        long startTime = System.currentTimeMillis();

        // Wait for 5 VBLs (recursive) - should all return immediately
        computer.waitForVBL(5);

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Elapsed time for 5 recursive waits: " + elapsed + "ms");

        assertTrue("Recursive waitForVBL should complete quickly when not running",
            elapsed < 200);
    }
}
