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
package jace;

import jace.apple2e.MOS65C02;
import jace.assembly.AssemblyHandler;
import jace.core.CPU;
import jace.core.Device;
import jace.ide.HeadlessProgram;
import jace.ide.Program;

/**
 *
 * @author brobert
 */
public class TestUtils {
    private TestUtils() {
        // Utility class has no constructor
    }

    public static void assemble(String code, int addr) {
        runAssemblyCode(code, addr, 0);
    }
    
    public static void runAssemblyCode(String code, int ticks) {
        runAssemblyCode(code, 0x0300, ticks);
    }
    
    public static void runAssemblyCode(String code, int addr, int ticks) {
        CPU cpu = Emulator.getComputer().getCpu();
        cpu.trace = true;
        HeadlessProgram program = new HeadlessProgram(Program.DocumentType.assembly);
        program.setValue("*=$"+Integer.toHexString(addr)+"\n "+code+"\n NOP\n RTS");
        program.execute();
        if (ticks > 0) {
            cpu.resume();
            for (int i=0; i < ticks; i++) {
                cpu.doTick();
            }
            cpu.suspend();
        }
    }

    public static Device createSimpleDevice(Runnable r, String name) {
        return new Device(Emulator.getComputer()) {
            @Override
            public void tick() {
                r.run();
            }
            
            @Override
            public String getShortName() {
                return name;
            }
            
            @Override
            public void reconfigure() {
            }
            
            @Override
            protected String getDeviceName() {
                return name;
            }
        };
    }
}
