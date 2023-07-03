/*
 * Copyright 2023 org.badvision.
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
import jace.core.Computer;
import jace.core.Utility;
import jace.core.SoundMixer;
import jace.apple2e.MOS65C02;
import jace.apple2e.RAM128k;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static jace.TestUtils.*;


/**
 * More advanced cycle counting tests.  These help ensure CPU runs correctly so things
 * like vapor lock and speaker sound work as expected.
 * @author brobert
 */
public class CycleCountTest {

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

        // Let the final stopping point for tests loop indefinitely
        // This also lets us figure out how long it's been looping when the test ends
        assemble("jmp $1000", 0x01000);
    }

    private String BELL = """
SPKR = $C030
BELL    ldy #$01
BELL2   lda #$0c
        jsr WAIT
        lda SPKR
        dey
        bne BELL2
        jmp $1000 ; Success case is jumping to $1000
WAIT    sec
WAIT2   pha
WAIT3   sbc #$01
        bne WAIT3
        pla
        sbc #$01
        bne WAIT2
        rts
""";
    
    
    /**
     * Test that the wait routine for beep takes ~552 cycles
     * Calling WAIT with A=#$c (12) should take 535 cycles
     * according to the tech ref notes: =1/2*(26+27*A+5*A^2) where A = 12 (0x0c)
     * The BELL routine has an additional 17 cycles per iteration
     * 
     * This test manually triggers the CPU to run directly with no other emulation occurring.
     */
    @Test
    public void testDirectBeeperCycleCount() {
        AtomicInteger breakpointEncountered = new AtomicInteger();
        AtomicInteger cycleCount = new AtomicInteger();
        // This listener will increment our breakpoint counter if it reaches our desired stoppoing point in time
        ram.addExecutionTrap(0x01000, e -> breakpointEncountered.incrementAndGet());

        // This faux device counts the number of cycles executed
        cpu.addChildDevice(createSimpleDevice(()->{
            if (breakpointEncountered.get() == 0) {
                cycleCount.incrementAndGet();
                System.out.print("*");
            }
        }, "Cycle Counter"));

        // Now run the CPU and see if we got the expected results
        cpu.resume();
        runAssemblyCode(BELL, 552);
        assertEquals("Should have encountered the breakpoint", 1, breakpointEncountered.get());
        assertEquals("Should have taken about 551 cycles to complete", 551, cycleCount.get());
        cpu.suspend();
    }
    
    /**
     * This is the same test as before except steps are executed from the motherboard
     */
    @Test
    public void testMachineBeeperCycleCount() {
        AtomicInteger breakpointEncountered = new AtomicInteger();
        AtomicInteger cycleCount = new AtomicInteger();
        // This listener will increment our breakpoint counter if it reaches our desired stoppoing point in time
        ram.addExecutionTrap(0x01000, e -> breakpointEncountered.incrementAndGet());

        // This faux device counts the number of cycles executed
        cpu.addChildDevice(createSimpleDevice(()->{
            if (breakpointEncountered.get() == 0) {
                cycleCount.incrementAndGet();
                System.out.print("*");
            }
        }, "Cycle Counter"));

        // This assembles the code and sets PC but doesn't actually do anything
        assemble(BELL, 0x0300);
        Emulator.getComputer().getMotherboard().resumeInThread();
        for (int i=0; i < 552; i++) {
            Emulator.getComputer().getMotherboard().doTick();
        }
        Emulator.getComputer().getMotherboard().suspend();
        
        assertEquals("Should have encountered the breakpoint", 1, breakpointEncountered.get());
        assertEquals("Should have taken about 551 cycles to complete", 551, cycleCount.get());    
    }
    
    // The CPU cycle count should work the same even when the emulator is sped up.
    @Test
    public void testAcceleratedCycleCount() {
        Emulator.getComputer().getMotherboard().setMaxSpeed(true);
        Emulator.getComputer().getMotherboard().setSpeedInPercentage(20000);
        testMachineBeeperCycleCount();
        Emulator.getComputer().getMotherboard().setMaxSpeed(false);
        Emulator.getComputer().getMotherboard().setSpeedInPercentage(100);
    }
    
    
}
