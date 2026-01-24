package jace.lawless;

import jace.core.Utility;
import jace.hardware.massStorage.image.ProDOSDiskImage;
import jace.library.MediaEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles automatic detection and upgrading of the game disk.
 * Integrates with existing LawlessImageTool upgrade logic.
 */
public class UpgradeHandler {

    private static final Logger LOGGER = Logger.getLogger(UpgradeHandler.class.getName());

    // Synchronization latch to coordinate upgrade completion with boot start
    private final CountDownLatch upgradeCompletionLatch = new CountDownLatch(1);

    public enum UpgradeDecision {
        UPGRADE,
        SKIP,
        CANCEL
    }

    private final LawlessImageTool imageTool;
    private final GameVersionTracker versionTracker;

    public UpgradeHandler(LawlessImageTool imageTool, GameVersionTracker versionTracker) {
        this.imageTool = imageTool;
        this.versionTracker = versionTracker;
    }

    /**
     * Shows a confirmation dialog asking the user if they want to upgrade.
     * This is a blocking call that returns after the user makes a decision.
     *
     * @return The user's decision
     */
    public UpgradeDecision showUpgradeConfirmation() {
        if (Utility.isHeadlessMode()) {
            return UpgradeDecision.SKIP;
        }

        final UpgradeDecision[] decision = {UpgradeDecision.CANCEL};
        final Object lock = new Object();

        javafx.application.Platform.runLater(() -> {
            synchronized (lock) {
                try {
                    javafx.scene.control.ButtonType upgradeButton =
                        new javafx.scene.control.ButtonType("Upgrade", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                    javafx.scene.control.ButtonType skipButton =
                        new javafx.scene.control.ButtonType("Skip", javafx.scene.control.ButtonBar.ButtonData.NO);
                    javafx.scene.control.ButtonType cancelButton =
                        new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        "A new game version has been detected. Would you like to upgrade and preserve your save game?\n\n" +
                        "Upgrade: Install new version and attempt to preserve your progress.\n" +
                        "Skip: Continue with current version.\n" +
                        "Cancel: Exit the game.",
                        upgradeButton, skipButton, cancelButton
                    );
                    alert.setTitle("Game Update Available");
                    alert.setHeaderText("New Game Version Detected");

                    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
                        if (result.get() == upgradeButton) {
                            decision[0] = UpgradeDecision.UPGRADE;
                        } else if (result.get() == skipButton) {
                            decision[0] = UpgradeDecision.SKIP;
                        } else {
                            decision[0] = UpgradeDecision.CANCEL;
                        }
                    }
                } finally {
                    lock.notifyAll();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for upgrade decision", e);
                Thread.currentThread().interrupt();
                return UpgradeDecision.CANCEL;
            }
        }

        return decision[0];
    }

    /**
     * Creates a backup copy of the game disk before upgrading.
     *
     * @param gameFile The game disk file to backup
     * @return The backup file, or null if backup failed
     */
    public File createBackup(File gameFile) {
        if (gameFile == null || !gameFile.exists()) {
            LOGGER.warning("Cannot create backup: game file does not exist");
            return null;
        }

        File backupFile = new File(gameFile.getParentFile(), gameFile.getName() + ".backup");
        try {
            Files.copy(gameFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Created backup at: " + backupFile.getAbsolutePath());

            // Verify backup integrity
            if (backupFile.length() != gameFile.length()) {
                LOGGER.severe("Backup file size mismatch - backup may be corrupted");
                backupFile.delete();
                return null;
            }

            return backupFile;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create backup", e);
            return null;
        }
    }

    /**
     * Restores the game disk from backup.
     *
     * @param gameFile The game disk file to restore
     * @param backupFile The backup file to restore from
     * @return true if restore was successful
     */
    public boolean restoreFromBackup(File gameFile, File backupFile) {
        if (backupFile == null || !backupFile.exists()) {
            LOGGER.severe("Cannot restore: backup file does not exist");
            return false;
        }

        try {
            Files.copy(backupFile.toPath(), gameFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Restored game from backup");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to restore from backup", e);
            return false;
        }
    }

    /**
     * Checks for updates on startup and handles the upgrade flow if needed.
     * Now checks if storage file was just replaced (via needsUpgrade flag set by getGamePath).
     *
     * @param storageGameFile The game disk file in storage
     * @param wasJustReplaced True if the storage file was just replaced by getGamePath()
     * @return true if the game should continue booting, false if it should exit
     */
    public boolean checkAndHandleUpgrade(File storageGameFile, boolean wasJustReplaced) {
        try {
            if (storageGameFile == null || !storageGameFile.exists()) {
                LOGGER.warning("Storage game file does not exist - skipping upgrade check");
                return true;
            }

            if (wasJustReplaced) {
                LOGGER.info("Storage file was replaced with newer packaged game - performing silent upgrade");
                return performUpgrade(storageGameFile);
            }

            // No upgrade needed - just keep .lkg backup synchronized with latest progress
            LOGGER.info("Game is current - no upgrade needed");
            createOrUpdateLastKnownGoodBackup(storageGameFile);
            return true;
        } finally {
            // Always signal completion (whether upgrade happened or not)
            upgradeCompletionLatch.countDown();
            LOGGER.info("Upgrade check complete - boot can proceed");
        }
    }

    /**
     * Waits for upgrade completion before allowing boot to proceed.
     * This prevents the boot watchdog from starting before upgrade finishes.
     *
     * @param timeoutMs Maximum time to wait in milliseconds (default: 10000ms)
     * @return true if upgrade completed within timeout, false if timeout occurred
     */
    public boolean awaitUpgradeCompletion(long timeoutMs) {
        try {
            LOGGER.info("Waiting for upgrade completion (timeout: " + timeoutMs + "ms)");
            boolean completed = upgradeCompletionLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (completed) {
                LOGGER.info("Upgrade completion confirmed - proceeding with boot");
            } else {
                LOGGER.warning("Upgrade completion timeout after " + timeoutMs + "ms - proceeding anyway");
            }
            return completed;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for upgrade completion", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Creates or updates the "last known good" backup.
     * This backup is used during upgrades to extract the save game.
     *
     * @param gameFile The current game file
     */
    private void createOrUpdateLastKnownGoodBackup(File gameFile) {
        File lkgBackup = getLastKnownGoodBackupFile(gameFile);
        try {
            Files.copy(gameFile.toPath(), lkgBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.fine("Updated last known good backup");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to update last known good backup", e);
        }
    }

    /**
     * Gets the file path for the last known good backup.
     *
     * @param gameFile The game file
     * @return The backup file path
     */
    private File getLastKnownGoodBackupFile(File gameFile) {
        return new File(gameFile.getParentFile(), gameFile.getName() + ".lkg");
    }

    /**
     * Performs a silent pre-boot upgrade by copying the save game file
     * from a backup to the new disk image using ProDOS utilities.
     * This is much faster than the orchestrated upgrade approach.
     *
     * @param gameFile The new game disk file to upgrade
     * @param backupFile The backup of the old game disk
     * @return true if successful, false if failed
     */
    private boolean performSilentUpgrade(File gameFile, File backupFile) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting silent upgrade - extracting save from backup: " + backupFile.getName());

        try {
            // Extract save from old disk using readFile
            byte[] saveGameData;
            try (ProDOSDiskImage reader = new ProDOSDiskImage(backupFile)) {
                saveGameData = reader.readFile("GAME.1.SAVE");
            }

            if (saveGameData == null) {
                LOGGER.info("No save game found in backup - booting clean");
                String version = GameVersionReader.extractVersion(gameFile);
                versionTracker.saveVersionInfo(gameFile.lastModified(), gameFile.length(), version);
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Silent upgrade completed (no save) in " + duration + "ms");
                return true;
            }

            LOGGER.info("Found save game (" + saveGameData.length + " bytes) - transferring to new disk: " + gameFile.getName());

            // Write save to new disk
            long writeStartTime = System.currentTimeMillis();
            try (ProDOSDiskImage writer = new ProDOSDiskImage(gameFile)) {
                writer.writeFile("GAME.1.SAVE", saveGameData, 0x00);
            }
            long writeDuration = System.currentTimeMillis() - writeStartTime;
            LOGGER.info("Save game write completed in " + writeDuration + "ms");

            // Success - update version info with size and version string
            String version = GameVersionReader.extractVersion(gameFile);
            versionTracker.saveVersionInfo(gameFile.lastModified(), gameFile.length(), version);

            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Silent upgrade completed successfully in " + duration + "ms");
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Silent upgrade failed", e);
            return false;
        }
    }

    /**
     * Verifies that the save game file is present in the disk image.
     * @param gameFile The game disk file to check
     * @return true if save game exists with non-zero size
     */
    private boolean verifySaveGamePresent(File gameFile) {
        try (ProDOSDiskImage verifier = new ProDOSDiskImage(gameFile)) {
            byte[] saveData = verifier.readFile("GAME.1.SAVE");
            if (saveData == null) {
                LOGGER.warning("Save game file not found during verification");
                return false;
            }
            if (saveData.length == 0) {
                LOGGER.warning("Save game file is empty during verification");
                return false;
            }
            LOGGER.info("Save game verified: " + saveData.length + " bytes present");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to verify save game", e);
            return false;
        }
    }

    /**
     * Performs the upgrade with backup and error handling.
     * Uses silent pre-boot upgrade (ProDOS file copy) instead of orchestrated upgrade.
     *
     * @param gameFile The game disk file to upgrade
     * @return true if successful or user wants to continue despite failure
     */
    private boolean performUpgrade(File gameFile) {
        // Get the last known good backup (contains the old version with save)
        File lkgBackup = getLastKnownGoodBackupFile(gameFile);
        if (!lkgBackup.exists()) {
            LOGGER.warning("No last known good backup found - cannot preserve save game");
            // Continue without upgrade - just record new version info
            try {
                String version = GameVersionReader.extractVersion(gameFile);
                versionTracker.saveVersionInfo(gameFile.lastModified(), gameFile.length(), version);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to update version info", e);
            }
            return true;
        }

        // Create a safety backup of the new version before modifying it
        File safetyBackup = createBackup(gameFile);
        if (safetyBackup == null) {
            LOGGER.warning("Failed to create safety backup - upgrade aborted");
            return true; // Continue with new version (no save)
        }

        try {
            // Attempt silent upgrade using last known good backup
            boolean success = performSilentUpgrade(gameFile, lkgBackup);

            if (success) {
                LOGGER.info("Upgrade completed - waiting for OS cache to settle");
                // Wait 100ms to allow OS file cache to settle before verification
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Interrupted during cache settle delay", e);
                    Thread.currentThread().interrupt();
                }

                // Verify that the save game was actually written
                boolean verified = verifySaveGamePresent(gameFile);
                if (!verified) {
                    LOGGER.warning("Save game verification failed - save may not have been retained");
                    // Continue anyway - don't restore, just warn
                }

                LOGGER.info("Upgrade completed successfully" + (verified ? " with verified save" : " (verification warning)"));
                // Update last known good backup to new version with save
                createOrUpdateLastKnownGoodBackup(gameFile);
                return true;
            } else {
                // Silent upgrade failed - restore from safety backup
                if (restoreFromBackup(gameFile, safetyBackup)) {
                    LOGGER.warning("Upgrade failed - restored to new version without save");
                } else {
                    LOGGER.severe("Upgrade failed and backup restoration failed");
                }
                return true; // Continue with new version (no save)
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Upgrade failed", e);

            // Attempt to restore from safety backup
            if (restoreFromBackup(gameFile, safetyBackup)) {
                LOGGER.warning("Upgrade failed - restored to new version without save");
            } else {
                LOGGER.severe("Upgrade failed and backup restoration failed");
            }
            return true; // Continue with new version (no save)
        }
    }
}
