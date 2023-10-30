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
package jace.core;

import java.util.HashSet;

import jace.apple2e.SoftSwitches;
import jace.apple2e.Speaker;
import jace.config.ConfigurableField;

/**
 * Motherboard is the heart of the computer. It can have a list of cards
 * inserted (the behavior and number of cards is determined by the Memory class)
 * as well as a speaker and any other miscellaneous devices (e.g. joysticks).
 * This class provides the real main loop of the emulator, and is responsible
 * for all timing as well as the pause/resume features used to prevent resource
 * collisions between threads. Created on May 1, 2007, 11:22 PM
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class Motherboard extends TimedDevice {

    @ConfigurableField(name = "Enable Speaker", shortName = "speaker", defaultValue = "true")
    public static boolean enableSpeaker = true;
    private Speaker speaker;

    void vblankEnd() {
        SoftSwitches.VBL.getSwitch().setState(true);
        computer.notifyVBLStateChanged(true);
    }

    void vblankStart() {
        SoftSwitches.VBL.getSwitch().setState(false);
        computer.notifyVBLStateChanged(false);
    }

    /**
     * Creates a new instance of Motherboard
     * @param computer
     * @param oldMotherboard
     */
    public Motherboard(Computer computer, Motherboard oldMotherboard) {
        super(computer);
        if (oldMotherboard != null) {
            addAllDevices(oldMotherboard.getChildren());
            speaker = oldMotherboard.speaker;
            accelorationRequestors.addAll(oldMotherboard.accelorationRequestors);
            setSpeedInHz(oldMotherboard.getSpeedInHz());
            setMaxSpeed(oldMotherboard.isMaxSpeed());
        }
    }

    @Override
    protected String getDeviceName() {
        return "Motherboard";
    }

    @Override
    public String getShortName() {
        return "mb";
    }
    @ConfigurableField(category = "advanced", shortName = "cpuPerClock", name = "CPU per clock", defaultValue = "1", description = "Number of extra CPU cycles per clock cycle (normal = 1)")
    public static int cpuPerClock = 0;
    public int clockCounter = 1;

    @Override
    public void tick() {
        // Extra CPU cycles requested, other devices are called by the TimedDevice abstraction
        for (int i=1; i < cpuPerClock; i++) {
            computer.getCpu().doTick();
            if (Speaker.force1mhz) {
                speaker.tick();
            }
        }
        /*
        try {
            clockCounter--;
            computer.getCpu().doTick();
            if (clockCounter > 0) {
                return;
            }
            clockCounter = cpuPerClock;
            computer.getVideo().doTick();
            Optional<Card>[] cards = computer.getMemory().getAllCards();
            for (Optional<Card> card : cards) {
                card.ifPresent(Card::doTick);
            }
        } catch (Throwable t) {
            System.out.print("!");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, t);
        }
*/
    }
    // From the holy word of Sather 3:5 (Table 3.1) :-)
    // This average speed averages in the "long" cycles
    public static final long DEFAULT_SPEED = 1020484L; // (NTSC)
    //public static long SPEED = 1015625L; // (PAL)

    @Override
    public long defaultCyclesPerSecond() {
        return DEFAULT_SPEED;
    }

    @Override
    public synchronized void reconfigure() {
        whileSuspended(() -> {
            accelorationRequestors.clear();
            super.reconfigure();

            // Now create devices as needed, e.g. sound

            if (enableSpeaker) {
                try {
                    if (speaker == null) {
                        speaker = new Speaker(computer);
                        speaker.attach();
                    }
                    speaker.reconfigure();
                    addChildDevice(speaker);
                } catch (Throwable t) {
                    System.out.println("Unable to initalize sound -- deactivating speaker out");
                    t.printStackTrace();
                }
            } else {
                System.out.println("Speaker not enabled, leaving it off.");
            }
        });
        adjustRelativeSpeeds();
    }
    HashSet<Object> accelorationRequestors = new HashSet<>();

    public void requestSpeed(Object requester) {
        accelorationRequestors.add(requester);
        enableTempMaxSpeed();
    }

    public void cancelSpeedRequest(Object requester) {
        accelorationRequestors.remove(requester);
        if (accelorationRequestors.isEmpty()) {
            disableTempMaxSpeed();
        }
    }
    
    void adjustRelativeSpeeds() {
        if (computer.getVideo() != null) {
            if (isMaxSpeed()) {
                computer.getVideo().setWaitPerCycle(8);
            } else if (getSpeedInHz() > DEFAULT_SPEED) {
                computer.getVideo().setWaitPerCycle(getSpeedInHz() / DEFAULT_SPEED);
            } else {
                computer.getVideo().setWaitPerCycle(0);            
            }
        }
    }
}
