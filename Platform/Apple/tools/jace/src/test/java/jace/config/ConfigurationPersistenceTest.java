package jace.config;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jace.EmulatorUILogic;
import javafx.embed.swing.JFXPanel;

/**
 * Verifies that UI configuration values survive a save / reload cycle.
 */
public class ConfigurationPersistenceTest {

    private File tempHome;
    private final Random rnd = new Random();

    @Before
    public void setup() throws Exception {
        // Initialize JavaFX toolkit (required for classes that touch Controls)
        new JFXPanel();
        // Use a fresh temp directory as the simulated user.home so we don't touch real files
        tempHome = Files.createTempDirectory("jace-conf-test").toFile();
        System.setProperty("user.home", tempHome.getAbsolutePath());
        // Ensure nothing left from previous runs
        File cfg = new File(tempHome, ".jace.conf");
        if (cfg.exists()) cfg.delete();
    }

    @After
    public void cleanup() throws Exception {
        // Delete temp directory recursively
        if (tempHome != null && tempHome.exists()) {
            for (File f : tempHome.listFiles()) f.delete();
            tempHome.delete();
        }
    }

    @Test
    public void uiSettingsPersistThroughSaveAndLoad() {
        // --- create initial configuration with random values ---
        Configuration.BASE = null; // start fresh
        Configuration.initializeBaseConfiguration();
        EmulatorUILogic ui = ((Configuration) Configuration.BASE.subject).ui;
        // populate with random data
        ui.windowWidth = 800 + rnd.nextInt(800);
        ui.windowHeight = 600 + rnd.nextInt(600);
        ui.windowSizeIndex = rnd.nextInt(4);
        ui.fullscreen = rnd.nextBoolean();
        ui.videoMode = rnd.nextBoolean() ? "Color" : "TextFriendly";
        ui.musicVolume = rnd.nextDouble();
        ui.sfxVolume = rnd.nextDouble();
        ui.soundtrackSelection = "track-" + rnd.nextInt(1000);
        ui.aspectRatioCorrection = rnd.nextBoolean();

        // save to disk
        Configuration.buildTree();
        Configuration.saveSettingsImmediate();

        // --- load into a fresh configuration object ---
        Configuration.BASE = null;
        Configuration.initializeBaseConfiguration();
        Configuration.buildTree();
        Configuration.loadSettings();
        EmulatorUILogic loaded = ((Configuration) Configuration.BASE.subject).ui;

        // assert equality
        assertEquals(ui.windowWidth, loaded.windowWidth);
        assertEquals(ui.windowHeight, loaded.windowHeight);
        assertEquals(ui.windowSizeIndex, loaded.windowSizeIndex);
        assertEquals(ui.fullscreen, loaded.fullscreen);
        assertEquals(ui.videoMode, loaded.videoMode);
        assertEquals(ui.musicVolume, loaded.musicVolume, 1e-6);
        assertEquals(ui.sfxVolume, loaded.sfxVolume, 1e-6);
        assertEquals(ui.soundtrackSelection, loaded.soundtrackSelection);
        assertEquals(ui.aspectRatioCorrection, loaded.aspectRatioCorrection);
    }
} 