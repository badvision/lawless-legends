/*
 * Copyright (C) 2012 Brendan Robert (BLuRry) brendan.robert@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jace.config.Configuration;
import jace.core.RAM;
import jace.lawless.LawlessComputer;

/**
 * Created on January 15, 2007, 10:10 PM
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */
public class Emulator {

    public static Emulator instance;
    private static EmulatorUILogic logic;
    public static Thread mainThread;

//    public static void main(String... args) {
//        mainThread = Thread.currentThread();
//        instance = new Emulator(args);
//    }

    private final LawlessComputer computer;

    public static EmulatorUILogic getUILogic() {
        if (logic == null) {
            logic = new EmulatorUILogic();
        }
        return logic;
    }
    
    public static Emulator getInstance(List<String> args) {
        Emulator i = getInstance();
        i.processCmdlineArgs(args);
        return i;
    }
    
    public static void abort() {
        if (instance != null) {
            if (instance.computer != null) {
                instance.computer.getMotherboard().suspend();
                instance.computer.getMotherboard().detach();
            }
        }
        instance = null;
    }
    
    public static Emulator getInstance() {
        if (instance == null) {
            instance = new Emulator();
        }
        return instance;
    }
    
    private static LawlessComputer getComputer() {
        return getInstance().computer;        
    }

    public static void whileSuspended(Consumer<LawlessComputer> action) {
        withComputer(c->c.getMotherboard().whileSuspended(()->action.accept(c)));
    }

    public static <T> T whileSuspended(Function<LawlessComputer, T> action, T defaultValue) {
        return withComputer(c->c.getMotherboard().whileSuspended(()->action.apply(c), defaultValue), defaultValue);
    }

    public static void withComputer(Consumer<LawlessComputer> c) {
        LawlessComputer computer = getComputer();
        if (computer != null) {
            c.accept(computer);
        } else {
            System.err.println("No computer available!");
            Thread.dumpStack();
        }
    }

    public static <T> T withComputer(Function<LawlessComputer, T> f, T defaultValue) {
        LawlessComputer computer = getComputer();
        if (computer != null) {
            return f.apply(computer);
        } else {
            System.err.println("No computer available!");
            Thread.dumpStack();
            return defaultValue;
        }
    }

    public static void withMemory(Consumer<RAM> m) {
        withComputer(c->{
            RAM memory = c.getMemory();
            if (memory != null) {
                m.accept(memory);
            } else {
                System.err.println("No memory available!");
                Thread.dumpStack();
            }
        });
    }

    public static void withVideo(Consumer<jace.core.Video> v) {
        withComputer(c->{
            jace.core.Video video = c.getVideo();
            if (video != null) {
                v.accept(video);
            } else {
                System.err.println("No video available!");
                Thread.dumpStack();
            }
        });
    }

    /**
     * Creates a new instance of Emulator
     */
    private Emulator() {
        instance = this;
        computer = new LawlessComputer();
        Configuration.buildTree();
        computer.getMotherboard().suspend();
        Configuration.loadSettings();
        mainThread = Thread.currentThread();
//        EmulatorUILogic.registerDebugger();
//        computer.coldStart();
    }

    private void processCmdlineArgs(List<String> args) {
        if (args == null || args.isEmpty()) {
            return;
        }
        Map<String, String> settings = new LinkedHashMap<>();
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).startsWith("-")) {
                String key = args.get(i).substring(1);
                if ((i + 1) < args.size()) {
                    String val = args.get(i + 1);
                    if (!val.startsWith("-")) {
                        settings.put(key, val);
                        i++;
                    } else {
                        settings.put(key, "true");
                    }
                } else {
                    settings.put(key, "true");
                }
            } else {
                System.err.println("Did not understand parameter " + args.get(i) + ", skipping.");
            }
        }
        Configuration.applySettings(settings);
    }
}