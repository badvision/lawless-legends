package jace.lawless;

import jace.AbstractFXTest;
import jace.Emulator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DeveloperBypassModeTest extends AbstractFXTest {

    @Before
    public void setUp() {
        // Clean up any existing emulator instance
        Emulator.abort();
        // Clear any existing system property
        System.clearProperty("jace.developerBypass");
    }

    @After
    public void tearDown() {
        // Clean up after tests
        Emulator.abort();
        System.clearProperty("jace.developerBypass");
    }

    @Test
    public void testBypassModeDisabled_ProductionModeEnabled() {
        // When bypass mode is NOT enabled
        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Then production mode should be enabled by default
        assertTrue("Production mode should be enabled without bypass",
                   computer.PRODUCTION_MODE);
        assertFalse("Developer bypass should be disabled by default",
                    computer.isDeveloperBypassMode());
    }

    @Test
    public void testBypassModeEnabled_ProductionModeDisabled() {
        // When bypass mode IS enabled via system property
        System.setProperty("jace.developerBypass", "true");

        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Then developer bypass mode should be enabled
        assertTrue("Developer bypass mode should be enabled",
                   computer.isDeveloperBypassMode());
    }

    @Test
    public void testBypassMode_SkipsProductionModeSetup() {
        // When bypass mode is enabled
        System.setProperty("jace.developerBypass", "true");

        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Initialize Lawless Legends configuration
        computer.initLawlessLegendsConfiguration();

        // Then production mode should be disabled
        assertFalse("Production mode should be disabled in bypass mode",
                    computer.PRODUCTION_MODE);
    }

    @Test
    public void testNormalMode_EnablesProductionMode() {
        // When bypass mode is NOT enabled
        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Initialize Lawless Legends configuration
        computer.initLawlessLegendsConfiguration();

        // Then production mode should be enabled
        assertTrue("Production mode should be enabled in normal mode",
                   computer.PRODUCTION_MODE);
    }

    @Test
    public void testBypassMode_SkipsBootAnimation() {
        // When bypass mode is enabled
        System.setProperty("jace.developerBypass", "true");

        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Initialize configuration
        computer.initLawlessLegendsConfiguration();

        // Then boot animation should be disabled
        assertFalse("Boot animation should be disabled in bypass mode",
                    computer.showBootAnimation);
    }

    @Test
    public void testNormalMode_ShowsBootAnimation() {
        // When bypass mode is NOT enabled (normal mode)
        Emulator emulator = Emulator.getInstance();
        LawlessComputer computer = (LawlessComputer) emulator.withComputer(c -> c, null);

        // Initialize configuration with production mode
        computer.PRODUCTION_MODE = true;
        computer.initLawlessLegendsConfiguration();

        // Boot animation setting should match production mode
        // (Note: showBootAnimation defaults to PRODUCTION_MODE value)
        assertEquals("Boot animation should match production mode setting",
                     computer.PRODUCTION_MODE, computer.showBootAnimation);
    }
}
