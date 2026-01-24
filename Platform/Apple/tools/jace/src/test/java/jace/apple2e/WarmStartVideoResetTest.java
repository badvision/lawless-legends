/*
 * Copyright 2024 org.badvision.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jace.apple2e;

import static jace.TestUtils.initComputer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jace.Emulator;
import jace.core.Computer;
import jace.core.SoundMixer;

/**
 * Test to verify that video switches are properly reset during warmStart.
 * Per Sather 4-15, authentic Apple IIe resets video mode to TEXT on warm reset.
 *
 * This test validates Phase 1 Fix #4: Reset video switches during warmStart
 *
 * @author Claude (BLuRry contribution)
 */
public class WarmStartVideoResetTest {

    @BeforeClass
    public static void setupClass() {
        initComputer();
        SoundMixer.MUTE = true;
    }

    @Before
    public void setUp() {
        Emulator.withComputer(Computer::reconfigure);
    }

    /**
     * Test that TEXT mode is enabled after warmStart.
     * Per authentic Apple IIe behavior, TEXT should be ON (true) after reset.
     */
    @Test
    public void testTextModeEnabledAfterWarmStart() {
        // Force TEXT mode off (graphics mode)
        SoftSwitches.TEXT.getSwitch().setState(false);
        assertFalse("TEXT should be disabled before warmStart", SoftSwitches.TEXT.getState());

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify TEXT mode is enabled after warmStart
        assertTrue("TEXT mode should be enabled after warmStart (authentic Apple IIe behavior per Sather 4-15)",
                   SoftSwitches.TEXT.getState());
    }

    /**
     * Test that HIRES mode is disabled after warmStart.
     * Initial state for HIRES is false.
     */
    @Test
    public void testHiresDisabledAfterWarmStart() {
        // Enable HIRES mode
        SoftSwitches.HIRES.getSwitch().setState(true);
        assertTrue("HIRES should be enabled before warmStart", SoftSwitches.HIRES.getState());

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify HIRES mode is disabled after warmStart
        assertFalse("HIRES mode should be disabled after warmStart (initial state is false)",
                    SoftSwitches.HIRES.getState());
    }

    /**
     * Test that MIXED mode is disabled after warmStart.
     * Initial state for MIXED is false.
     */
    @Test
    public void testMixedDisabledAfterWarmStart() {
        // Enable MIXED mode
        SoftSwitches.MIXED.getSwitch().setState(true);
        assertTrue("MIXED should be enabled before warmStart", SoftSwitches.MIXED.getState());

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify MIXED mode is disabled after warmStart
        assertFalse("MIXED mode should be disabled after warmStart (initial state is false)",
                    SoftSwitches.MIXED.getState());
    }

    /**
     * Test that DHIRES (double hi-res) mode is disabled after warmStart.
     * Initial state for DHIRES is false.
     */
    @Test
    public void testDhiresDisabledAfterWarmStart() {
        // Enable DHIRES mode
        SoftSwitches.DHIRES.getSwitch().setState(true);
        assertTrue("DHIRES should be enabled before warmStart", SoftSwitches.DHIRES.getState());

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify DHIRES mode is disabled after warmStart
        assertFalse("DHIRES mode should be disabled after warmStart (initial state is false)",
                    SoftSwitches.DHIRES.getState());
    }

    /**
     * Test that _80COL (80 column) mode is disabled after warmStart.
     * Initial state for _80COL is false.
     */
    @Test
    public void test80ColDisabledAfterWarmStart() {
        // Enable 80 column mode
        SoftSwitches._80COL.getSwitch().setState(true);
        assertTrue("80COL should be enabled before warmStart", SoftSwitches._80COL.getState());

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify 80 column mode is disabled after warmStart
        assertFalse("80COL mode should be disabled after warmStart (initial state is false)",
                    SoftSwitches._80COL.getState());
    }

    /**
     * Comprehensive test that all video switches return to their initial states.
     */
    @Test
    public void testAllVideoSwitchesResetToInitialState() {
        // Manipulate all video switches to non-initial states
        SoftSwitches.TEXT.getSwitch().setState(false);    // Initial: true
        SoftSwitches.MIXED.getSwitch().setState(true);    // Initial: false
        SoftSwitches.PAGE2.getSwitch().setState(true);    // Initial: false
        SoftSwitches.HIRES.getSwitch().setState(true);    // Initial: false
        SoftSwitches.DHIRES.getSwitch().setState(true);   // Initial: false
        SoftSwitches._80COL.getSwitch().setState(true);   // Initial: false
        SoftSwitches.ALTCH.getSwitch().setState(true);    // Initial: false

        // Perform warm start
        Emulator.withComputer(Computer::warmStart);

        // Verify all video switches return to initial states
        assertTrue("TEXT should be true after warmStart", SoftSwitches.TEXT.getState());
        assertFalse("MIXED should be false after warmStart", SoftSwitches.MIXED.getState());
        assertFalse("PAGE2 should be false after warmStart", SoftSwitches.PAGE2.getState());
        assertFalse("HIRES should be false after warmStart", SoftSwitches.HIRES.getState());
        assertFalse("DHIRES should be false after warmStart", SoftSwitches.DHIRES.getState());
        assertFalse("80COL should be false after warmStart", SoftSwitches._80COL.getState());
        assertFalse("ALTCH should be false after warmStart", SoftSwitches.ALTCH.getState());
    }
}
