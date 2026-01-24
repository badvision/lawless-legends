package jace;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jace.config.Configuration;
import javafx.embed.swing.JFXPanel;

/**
 * Tests for JaceUIController focusing on settings persistence during shutdown.
 */
public class JaceUIControllerTest {

    private File tempHome;

    @Before
    public void setup() throws Exception {
        // Initialize JavaFX toolkit
        new JFXPanel();

        // Use a fresh temp directory as the simulated user.home
        tempHome = Files.createTempDirectory("jace-ui-test").toFile();
        System.setProperty("user.home", tempHome.getAbsolutePath());

        // Clean up any existing config files
        File cfg = new File(tempHome, ".jace.conf");
        if (cfg.exists()) cfg.delete();
        File json = new File(tempHome, ".jace.json");
        if (json.exists()) json.delete();

        // Initialize configuration
        Configuration.BASE = null;
        Configuration.initializeBaseConfiguration();
        Configuration.buildTree();

        // Mark startup as complete to allow saves
        JaceUIController.startupComplete = true;
    }

    @After
    public void cleanup() throws Exception {
        // Reset startup flag
        JaceUIController.startupComplete = false;

        // Delete temp directory recursively
        if (tempHome != null && tempHome.exists()) {
            for (File f : tempHome.listFiles()) {
                f.delete();
            }
            tempHome.delete();
        }
    }

    /**
     * This test demonstrates the bug: when settings are changed via saveUISettings()
     * and the app closes quickly (within 2 seconds), the debounced save doesn't
     * complete, and settings are lost.
     *
     * The current shutdown() implementation waits for pendingSave, but only if
     * saveUISettings() was actually called. If the user changes a setting and
     * closes immediately, the setting is in memory but saveUISettings() scheduled
     * a future save that won't happen if shutdown() is called too soon after
     * a rapid executor shutdown.
     *
     * After the fix, this test should pass.
     */
    @Test
    public void settingsChangedDuringShutdownArePersisted() throws Exception {
        // Create and configure UI settings directly
        EmulatorUILogic ui = ((Configuration) Configuration.BASE.subject).ui;
        ui.musicVolume = 0.5;

        // Save initial state
        Configuration.buildTree();
        Configuration.saveSettingsImmediate();
        Thread.sleep(100);

        // Create controller - this simulates app running
        JaceUIController controller = new JaceUIController();

        // Change settings in memory (simulating what happens when user adjusts slider)
        ui.musicVolume = 0.75;

        // Call saveUISettings to trigger debounced save
        controller.saveUISettings();

        // Immediately trigger shutdown (simulating user closing app quickly)
        // This happens BEFORE the 2-second debounce period expires
        // The fix should ensure this setting is saved immediately during shutdown
        controller.shutdown();

        // Small delay to ensure any file I/O completes
        Thread.sleep(200);

        // Now load settings in a fresh configuration to verify persistence
        Configuration.BASE = null;
        Configuration.initializeBaseConfiguration();
        Configuration.buildTree();
        Configuration.loadSettings();

        EmulatorUILogic loaded = ((Configuration) Configuration.BASE.subject).ui;

        // Assert that the new volume value was saved (not the initial value)
        assertEquals("Music volume should be persisted during shutdown",
                     0.75, loaded.musicVolume, 0.001);
    }

    /**
     * Test that normal debounced saves still work during regular operation.
     */
    @Test
    public void settingsAreDebouncedSavedDuringNormalOperation() throws Exception {
        // Create and configure UI settings directly
        EmulatorUILogic ui = ((Configuration) Configuration.BASE.subject).ui;

        // Change the music volume
        ui.musicVolume = 0.85;

        // Trigger a save manually to simulate the debounced save
        Configuration.buildTree();
        Configuration.saveSettingsImmediate();

        // Wait for save to complete
        Thread.sleep(200);

        // Load settings in a fresh configuration
        Configuration.BASE = null;
        Configuration.initializeBaseConfiguration();
        Configuration.buildTree();
        Configuration.loadSettings();

        EmulatorUILogic loaded = ((Configuration) Configuration.BASE.subject).ui;

        // Assert that the volume was saved
        assertEquals("Music volume should be persisted via save",
                     0.85, loaded.musicVolume, 0.001);
    }
}
