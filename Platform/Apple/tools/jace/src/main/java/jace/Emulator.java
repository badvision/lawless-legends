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

import jace.config.Configuration;
import jace.lawless.LawlessComputer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on January 15, 2007, 10:10 PM
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */
public class Emulator {

    public static Emulator instance;
    public static EmulatorUILogic logic = new EmulatorUILogic();
    public static Thread mainThread;

//    public static void main(String... args) {
//        mainThread = Thread.currentThread();
//        instance = new Emulator(args);
//    }

    private final LawlessComputer computer;
    
    public static Emulator getInstance(List<String> args) {
        Emulator i = getInstance();
        i.processCmdlineArgs(args);
        return i;
    }
    
    public static Emulator getInstance() {
        if (instance == null) {
            instance = new Emulator();
        }
        return instance;
    }
    
    public static LawlessComputer getComputer() {
        return getInstance().computer;        
    }

    /**
     * Creates a new instance of Emulator
     */
    private Emulator() {
        instance = this;
        computer = new LawlessComputer();
        Configuration.buildTree();
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