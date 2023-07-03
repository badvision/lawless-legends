/*
 * Copyright 2016 Brendan Robert
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
package jace.cpu;

import jace.Emulator;
import jace.apple2e.MOS65C02;
import jace.apple2e.RAM128k;
import jace.core.Computer;
import jace.core.SoundMixer;
import jace.core.Utility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static jace.TestUtils.*;

/**
 * Basic test functionality to assert correct 6502 decode and execution.
 *
 * @author blurry
 */
public class Basic6502FuncationalityTest {

    static Computer computer;
    static MOS65C02 cpu;
    static RAM128k ram;

    @BeforeClass
    public static void setupClass() {
        Utility.setHeadlessMode(true);
        SoundMixer.MUTE = true;
        computer = Emulator.getComputer();
        cpu = (MOS65C02) computer.getCpu();
        ram = (RAM128k) computer.getMemory();
    }

    @AfterClass
    public static void teardownClass() {
    }

    @Before
    public void setup() {
        computer.pause();
        cpu.clearState();
    }

    @Test
    public void assertMemoryConfiguredCorrectly() {
        assertArrayEquals("Active read bank 3 should be main memory page 3",
                ram.mainMemory.getMemoryPage(3),
                ram.activeRead.getMemoryPage(3));

        assertArrayEquals("Active write bank 3 should be main memory page 3",
                ram.mainMemory.getMemoryPage(3),
                ram.activeWrite.getMemoryPage(3));
    }

    @Test
    public void testAdditionNonDecimal() {
        cpu.A = 0;
        cpu.D = false;
        cpu.C = 0;
        runAssemblyCode("adc #1", 2);
        assertEquals("0+1 (c=0) = 1", 1, cpu.A);
        assertFalse("Result is not zero", cpu.Z);
        assertEquals("Carry is clear", 0, cpu.C);
    }

    @Test
    public void testAdditionNonDecimalWithCarry() {
        cpu.A = 0;
        cpu.D = false;
        cpu.C = 1;
        runAssemblyCode("adc #1", 2);
        assertEquals("0+1 (c=1) = 2", 2, cpu.A);
        assertFalse("Result is not zero", cpu.Z);
        assertEquals("Carry is clear", 0, cpu.C);
    }
    
    @Test
    public void testAdditionDecimal() {
        cpu.A = 9;
        cpu.D = true;
        cpu.C = 0;
        runAssemblyCode("adc #1", 2);
        assertEquals("9+1 (c=0) = 0x10", 0x10, cpu.A);
        assertFalse("Result is not zero", cpu.Z);
        assertEquals("Carry is clear", 0, cpu.C);
    }
    
    @Test
    public void testAdditionDecimalWithCarry() {
        cpu.A = 9;
        cpu.D = true;
        cpu.C = 1;
        runAssemblyCode("adc #1", 2);
        assertEquals("9+1 (c=1) = 0x11", 0x11, cpu.A);
        assertFalse("Result is not zero", cpu.Z);
        assertEquals("Carry is clear", 0, cpu.C);
    }
}
