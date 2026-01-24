package jace.lawless;

import jace.core.Utility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class UpgradeHandlerTest {

    private File tempDir;
    private File testGameFile;
    private GameVersionTracker tracker;
    private LawlessImageTool imageTool;
    private UpgradeHandler upgradeHandler;

    @Before
    public void setUp() throws IOException {
        Utility.setHeadlessMode(true);
        tempDir = Files.createTempDirectory("lawless-upgrade-test").toFile();
        testGameFile = new File(tempDir, "game.2mg");
        Files.writeString(testGameFile.toPath(), "test game content");
        tracker = new GameVersionTracker(tempDir);
        imageTool = new LawlessImageTool();
        upgradeHandler = new UpgradeHandler(imageTool, tracker);
    }

    @After
    public void tearDown() {
        deleteDirectory(tempDir);
        Utility.setHeadlessMode(false);
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
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

    @Test
    public void testCreateBackup_Success() {
        File backup = upgradeHandler.createBackup(testGameFile);

        assertNotNull("Backup should be created", backup);
        assertTrue("Backup file should exist", backup.exists());
        assertEquals("Backup should have same size as original",
            testGameFile.length(), backup.length());
        assertTrue("Backup should have .backup extension",
            backup.getName().endsWith(".backup"));
    }

    @Test
    public void testCreateBackup_NonexistentFile() {
        File nonexistent = new File(tempDir, "nonexistent.2mg");
        File backup = upgradeHandler.createBackup(nonexistent);

        assertNull("Backup of nonexistent file should return null", backup);
    }

    @Test
    public void testCreateBackup_NullFile() {
        File backup = upgradeHandler.createBackup(null);

        assertNull("Backup of null file should return null", backup);
    }

    @Test
    public void testRestoreFromBackup_Success() throws IOException {
        // Create backup
        File backup = upgradeHandler.createBackup(testGameFile);
        assertNotNull(backup);

        // Modify original
        Files.writeString(testGameFile.toPath(), "modified content");
        assertNotEquals("File should be modified",
            backup.length(), testGameFile.length());

        // Restore
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, backup);

        assertTrue("Restore should succeed", restored);
        assertEquals("File should match backup after restore",
            backup.length(), testGameFile.length());
    }

    @Test
    public void testRestoreFromBackup_NonexistentBackup() {
        File nonexistent = new File(tempDir, "nonexistent.backup");
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, nonexistent);

        assertFalse("Restore should fail with nonexistent backup", restored);
    }

    @Test
    public void testRestoreFromBackup_NullBackup() {
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, null);

        assertFalse("Restore should fail with null backup", restored);
    }

    @Test
    public void testCheckAndHandleUpgrade_NoUpgradeNeeded_UpdatesBackup() {
        // When not replaced, should just update .lkg backup
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile, false);

        assertTrue("Should continue when no upgrade needed", shouldContinue);

        // Verify .lkg backup was created
        File lkgBackup = new File(testGameFile.getParentFile(), testGameFile.getName() + ".lkg");
        assertTrue(".lkg backup should be created", lkgBackup.exists());
    }

    @Test
    public void testCheckAndHandleUpgrade_WasReplaced_PerformsUpgrade() throws Exception {
        // In real flow, .lkg is created by getGamePath() before upgrade runs
        // We simulate this by creating it first
        File lkgBackup = new File(testGameFile.getParentFile(), testGameFile.getName() + ".lkg");
        java.nio.file.Files.copy(testGameFile.toPath(), lkgBackup.toPath());

        // When wasJustReplaced=true, should perform upgrade
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile, true);

        assertTrue("Should continue after upgrade", shouldContinue);
        assertTrue(".lkg backup should exist (created by getGamePath)", lkgBackup.exists());
    }

    @Test
    public void testCheckAndHandleUpgrade_NonexistentFile() {
        File nonexistent = new File(tempDir, "nonexistent.2mg");
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(nonexistent, false);

        assertTrue("Should continue even with nonexistent file", shouldContinue);
    }

    @Test
    public void testCheckAndHandleUpgrade_NullFile() {
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(null, false);

        assertTrue("Should continue even with null file", shouldContinue);
    }

    @Test
    public void testShowUpgradeConfirmation_HeadlessMode_ReturnsSkip() {
        Utility.setHeadlessMode(true);
        UpgradeHandler.UpgradeDecision decision = upgradeHandler.showUpgradeConfirmation();

        assertEquals("Headless mode should return SKIP",
            UpgradeHandler.UpgradeDecision.SKIP, decision);
    }

    @Test(timeout = 5000)
    public void testAwaitUpgradeCompletion_SignalsImmediatelyAfterCheckAndHandleUpgrade() throws Exception {
        // This test verifies that checkAndHandleUpgrade() signals the latch
        // and awaitUpgradeCompletion() returns immediately

        // Start a thread that will call checkAndHandleUpgrade
        Thread upgradeThread = new Thread(() -> {
            upgradeHandler.checkAndHandleUpgrade(testGameFile, false);
        });

        // Start a thread that waits for completion
        final boolean[] completedWithinTimeout = new boolean[1];
        Thread waitingThread = new Thread(() -> {
            completedWithinTimeout[0] = upgradeHandler.awaitUpgradeCompletion(10000);
        });

        // Start both threads
        waitingThread.start();
        Thread.sleep(100); // Give waiting thread time to start waiting
        upgradeThread.start();

        // Wait for both to complete
        upgradeThread.join(2000);
        waitingThread.join(2000);

        assertTrue("Upgrade completion should be signaled", completedWithinTimeout[0]);
    }

    @Test(timeout = 2000)
    public void testAwaitUpgradeCompletion_TimeoutWhenNotSignaled() {
        // Create a fresh UpgradeHandler that hasn't signaled
        UpgradeHandler freshHandler = new UpgradeHandler(imageTool, tracker);

        // This should timeout quickly
        long startTime = System.currentTimeMillis();
        boolean completed = freshHandler.awaitUpgradeCompletion(500);
        long duration = System.currentTimeMillis() - startTime;

        assertFalse("Should timeout when upgrade not complete", completed);
        assertTrue("Should wait approximately the timeout duration", duration >= 450 && duration < 1000);
    }

    @Test(timeout = 5000)
    public void testSynchronization_BootWatchdogWaitsForUpgrade() throws Exception {
        // Simulate the race condition scenario from RC-1
        // This test verifies that boot watchdog waits for upgrade to complete

        final long[] upgradeStartTime = new long[1];
        final long[] upgradeEndTime = new long[1];
        final long[] bootStartTime = new long[1];

        // Create a new UpgradeHandler for this test
        UpgradeHandler testHandler = new UpgradeHandler(imageTool, tracker);

        // Thread 1: Simulate upgrade with delay
        Thread upgradeThread = new Thread(() -> {
            upgradeStartTime[0] = System.currentTimeMillis();
            try {
                Thread.sleep(200); // Simulate 200ms upgrade time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            testHandler.checkAndHandleUpgrade(testGameFile, false);
            upgradeEndTime[0] = System.currentTimeMillis();
        });

        // Thread 2: Simulate boot watchdog waiting for upgrade
        Thread bootThread = new Thread(() -> {
            testHandler.awaitUpgradeCompletion(10000);
            bootStartTime[0] = System.currentTimeMillis();
        });

        // Start both threads
        upgradeThread.start();
        bootThread.start();

        // Wait for both to complete
        upgradeThread.join(5000);
        bootThread.join(5000);

        // Verify that boot started AFTER upgrade completed
        assertTrue("Upgrade should complete", upgradeEndTime[0] > 0);
        assertTrue("Boot should start", bootStartTime[0] > 0);
        assertTrue("Boot should start AFTER upgrade completes",
            bootStartTime[0] >= upgradeEndTime[0]);

        long raceWindow = bootStartTime[0] - upgradeEndTime[0];
        assertTrue("Boot should start within 50ms of upgrade completion",
            raceWindow < 50);
    }
}
