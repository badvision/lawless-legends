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
package jace.hardware;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.Emulator;
import jace.config.ConfigurableField;
import jace.config.Name;
import jace.core.Card;
import jace.core.Computer;
import jace.core.Motherboard;
import jace.core.RAMEvent;
import jace.core.RAMEvent.TYPE;
import jace.core.RAMListener;
import jace.core.SoundMixer;
import jace.core.SoundMixer.SoundBuffer;
import jace.core.SoundMixer.SoundError;
import jace.hardware.mockingboard.PSG;
import jace.hardware.mockingboard.R6522;

/**
 * Mockingboard-C implementation (with partial Phasor support). This uses two
 * 6522 chips to communicate to two respective AY PSG sound chips. This class
 * manages the I/O access as well as the sound playback thread.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
@Name("Mockingboard")
public class CardMockingboard extends Card implements Runnable {
    // If true, emulation will cover 4 AY chips.  Otherwise, only 2 AY chips

    @ConfigurableField(name = "Volume", shortName = "vol",
            category = "Sound",
            description = "Mockingboard volume, 100=max, 0=silent")
    public int volume = 100;
    static public int MAX_AMPLITUDE = 0x007fff;
    @ConfigurableField(name = "Phasor mode",
            category = "Sound",
            description = "If enabled, card will have 4 sound chips instead of 2")
    public boolean phasorMode = false;
    @ConfigurableField(name = "Clock Rate (hz)",
            category = "Sound",
            defaultValue = "1020484",
            description = "Clock rate of AY oscillators")
    public int CLOCK_SPEED = 1020484;
    public int SAMPLE_RATE = 48000;
    @ConfigurableField(name = "Buffer size",
            category = "Sound",
            description = "Number of samples to generate on each pass")
    public int BUFFER_LENGTH = 2;
    // The array of configured AY chips
    public PSG[] chips;
    // The 6522 controllr chips (always 2)
    public R6522[] controllers;
    static private int ticksBetweenPlayback = 200;
    Lock timerSync = new ReentrantLock();
    Condition cpuCountReached = timerSync.newCondition();
    Condition playbackFinished = timerSync.newCondition();
    @ConfigurableField(name = "Idle sample threshold", description = "Number of samples to wait before suspending sound")
    private final int MAX_IDLE_SAMPLES = SAMPLE_RATE;
    
    @Override
    public String getDeviceName() {
        return "Mockingboard";
    }

    public CardMockingboard() {
        super();
        controllers = new R6522[2];
        for (int i = 0; i < 2; i++) {
            // has to be final to be used inside of anonymous class below
            final int j = i;
            controllers[i] = new R6522() {
                final int controller = j;

                @Override
                public void sendOutputA(int value) {
                    chips[j].setBus(value);
                    if (phasorMode) {
                        chips[j + 2].setBus(value);
                    }
                }

                @Override
                public void sendOutputB(int value) {
                    if (phasorMode) {
                        if ((chips[j].mask & value) != 0) {
                            chips[j].setControl(value & 0x07);
                        }
                        if ((chips[j + 2].mask & value) != 0) {
                            chips[j + 2].setControl(value & 0x07);
                        }
                    } else {
                        chips[j].setControl(value & 0x07);
                    }
                }

                @Override
                public int receiveOutputA() {
                    return chips[j] == null ? 0 : chips[j].bus;
                }

                @Override
                public int receiveOutputB() {
                    return 0;
                }

                @Override
                public String getShortName() {
                    return "timer" + j;
                }
                
                public void tick() {
                    super.tick();
                    if (controller == 0) {
                        doSoundTick();
                    }
                }
            };
        }
    }

    @Override
    public void reset() {
        suspend();
    }
    RAMListener mainListener = null;
    
    boolean heatbeatUnclocked = false;
    long heartbeatReclockTime = 0L;
    long unclockTime = 5000L;
    
    private void setUnclocked(boolean unclocked) {
        heatbeatUnclocked = unclocked;
        for (R6522 controller : controllers) {
            controller.setUnclocked(unclocked);
        }
        heartbeatReclockTime = System.currentTimeMillis() + unclockTime;
    }

    @Override
    protected void handleFirmwareAccess(int register, TYPE type, int value, RAMEvent e) {
        resume();
        int chip = 0;
        for (PSG psg : chips) {
            if (psg.getBaseReg() == (register & 0x0f0)) {
                break;
            }
            chip++;
        }
        if (chip >= 2) {
            System.err.println("Could not determine which PSG to communicate to for access to regsiter + " + Integer.toHexString(register));
            Emulator.withVideo(v->e.setNewValue(v.getFloatingBus()));
            return;
        }
        R6522 controller = controllers[chip & 1];
        if (e.getType().isRead()) {
            int val = controller.readRegister(register & 0x0f);
            e.setNewValue(val);
//            System.out.println("Read "+Integer.toHexString(register)+" == "+val);
        } else {
            controller.writeRegister(register & 0x0f, e.getNewValue());
//            System.out.println("Write "+Integer.toHexString(register)+" == "+e.getNewValue());
        }
    }

    @Override
    protected void handleIOAccess(int register, TYPE type, int value, RAMEvent e) {
        // Oddly, all IO is done at the firmware address bank.  It's a strange card.
//        System.out.println("MB I/O Access "+type.name()+" "+register+":"+value);
        Emulator.withVideo(v->e.setNewValue(v.getFloatingBus()));
    }
    long ticksSinceLastPlayback = 0;

    @Override
    public void tick() {
        if (heatbeatUnclocked) {
            if (System.currentTimeMillis() - heartbeatReclockTime >= unclockTime) {
                setUnclocked(false);
            } else {
                for (R6522 c : controllers) {
                    if (c == null || !c.isRunning()) {
                        continue;
                    }
                    c.doTick();
                }
            }
        }
    }
    
    public boolean isRunning() {
        return super.isRunning() && playbackThread != null && playbackThread.isAlive();
    }
    
    private void doSoundTick() {
        if (isRunning() && !pause) {
//            buildMixerTable();
            timerSync.lock();
            try {
                ticksSinceLastPlayback++;
                if (ticksSinceLastPlayback >= ticksBetweenPlayback) {
                    cpuCountReached.signalAll();
                    while (isRunning() && ticksSinceLastPlayback >= ticksBetweenPlayback) {
                        if (!playbackFinished.await(1, TimeUnit.SECONDS)) {
//                            gripe("The mockingboard playback thread has stalled.  Disabling mockingboard.");
                            suspendSound();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                suspend();
                // Do nothing, probably suspending CPU
            } finally {
                timerSync.unlock();
            }
        }
    }

    @Override
    public void reconfigure() {
        boolean restart = suspend();
        initPSG();
        for (PSG chip : chips) {
            chip.setRate(phasorMode ? CLOCK_SPEED * 2 : CLOCK_SPEED, SAMPLE_RATE);
            chip.reset();
        }
        super.reconfigure();
        if (restart) {
            resume();
        }
    }

///////////////////////////////////////////////////////////
    public static int[] VolTable;

    public void playSound(int[] left, int[] right) {
        chips[0].update(left, true, left, false, left, false, BUFFER_LENGTH);
        chips[1].update(right, true, right, false, right, false, BUFFER_LENGTH);
        if (phasorMode) {
            chips[2].update(left, false, left, false, left, false, BUFFER_LENGTH);
            chips[3].update(right, false, right, false, right, false, BUFFER_LENGTH);
        }
    }

    public void buildMixerTable() {
        VolTable = new int[16];
        int numChips = phasorMode ? 4 : 2;

        /* calculate the volume->voltage conversion table */
        /* The AY-3-8910 has 16 levels, in a logarithmic scale (3dB per step) */
        /* The YM2149 still has 16 levels for the tone generators, but 32 for */
        /* the envelope generator (1.5dB per step). */
        double out = (MAX_AMPLITUDE * volume) / 100.0;
        // Reduce max amplitude to reflect post-mixer values so we don't have to scale volume when mixing channels
        out = out * 2.0 / 3.0 / numChips;
        // double delta = 1.15;
        for (int i = 15; i > 0; i--) {
            VolTable[i] = (int) (out / Math.pow(Math.sqrt(2),(15-i)));
//            out /= 1.188502227;	/* = 10 ^ (1.5/20) = 1.5dB */
//            out /= 1.15;	/* = 10 ^ (3/20) = 3dB */
//            delta += 0.0225;
//            out /= delta;   // As per applewin's source, the levels don't scale as documented.
        }
        
        VolTable[0] = 0;
    }
    Thread playbackThread = null;
    boolean pause = false;

    @Override
    public void resume() {
        pause = false;
        if (chips == null) {
            initPSG();
            for (PSG psg : chips) {
                psg.setRate(phasorMode ? CLOCK_SPEED * 2 : CLOCK_SPEED, SAMPLE_RATE);
                psg.reset();
            }
        }
        if (!isRunning()) {
            setUnclocked(true);
            for (R6522 controller : controllers) {
                controller.attach();
                controller.resume();
            }
        }
        super.resume();
        if (playbackThread == null || !playbackThread.isAlive()) {
            playbackThread = new Thread(this, "Mockingboard sound playback");
            playbackThread.start();
        }
    }

    @Override
    public boolean suspend() {
        super.suspend();
        for (R6522 controller : controllers) {
            controller.suspend();
            controller.detach();
        }
        return suspendSound();
    }
    
    public boolean suspendSound() {
        setRun(false);
        if (playbackThread == null || !playbackThread.isAlive()) {
            return false;
        }
        if (playbackThread != null) {
            try {
                playbackThread.join(500);
            } catch (InterruptedException ex) {
            }
        }
        playbackThread = null;
        return true;
    }

    @Override
    /**
     * This is the audio playback thread
     */
    public void run() {
        SoundBuffer buffer;
        try {
            buffer = SoundMixer.createBuffer(true);
        } catch (InterruptedException | ExecutionException | SoundError e) {
            e.printStackTrace();
            setRun(false);
            return;
        }
        try {
            if (buffer == null) {
                setRun(false);
                return;
            }
            System.out.println("Mockingboard playback started");
            int bufferSize  = SoundMixer.BUFFER_SIZE;
            int[] left = new int[bufferSize];
            int[] right = new int[bufferSize];
            buildMixerTable();
            ticksBetweenPlayback = (int) ((Motherboard.DEFAULT_SPEED * BUFFER_LENGTH) / SAMPLE_RATE);
            System.out.println("Ticks between playback: "+ticksBetweenPlayback);
            ticksSinceLastPlayback = 0;
            int zeroSamples = 0;
            setRun(true);
            LockSupport.parkNanos(5000);
            while (isRunning() && !Thread.interrupted()) {
                while (isRunning() && !Emulator.withComputer(Computer::isRunning, false)) {
                    Thread.sleep(1000);
                }
                if (isRunning() && !Thread.interrupted()) {
                    playSound(left, right);
                    try {
                        for (int i=0; i < bufferSize; i++) {
                            buffer.playSample((short) left[i]);
                            buffer.playSample((short) right[i]);
                        }
                        timerSync.lock();
                        ticksSinceLastPlayback -= ticksBetweenPlayback;
                    } catch (ExecutionException | SoundError e) {
                        Logger.getLogger(CardMockingboard.class.getName()).log(Level.SEVERE, "Mockingboard playback encountered fatal exception", e);
                        try {
                            buffer.shutdown();
                        } catch (ExecutionException | SoundError e1) {
                            // Ignore shutdown errors, we're already reporting a fatal error
                        }
                        buffer=null;
                        setRun(false);
                        break;
                    } finally {
                        timerSync.unlock();
                    }
                    if (zeroSamples >= MAX_IDLE_SAMPLES) {
                        zeroSamples = 0;
                        pause = true;
                        Emulator.withComputer(c->c.getMotherboard().cancelSpeedRequest(this));
                        while (pause && isRunning()) {
                            try {
                                Thread.sleep(50);
                                timerSync.lock();
                                playbackFinished.signalAll();
                            } catch (InterruptedException ex) {
                                return;
                            } catch (IllegalMonitorStateException ex) {
                                // Do nothing
                            } finally {
                                try {
                                    timerSync.unlock();
                                } catch (IllegalMonitorStateException ex) {
                                    // Do nothing -- this is probably caused by a suspension event
                                }
                            }
                        }
                    }
                    try {
                        timerSync.lock();
                        playbackFinished.signalAll();
                        while (isRunning() && ticksSinceLastPlayback < ticksBetweenPlayback) {
                            Emulator.withComputer(c->c.getMotherboard().requestSpeed(this));
                            cpuCountReached.await();
                            Emulator.withComputer(c->c.getMotherboard().cancelSpeedRequest(this));
                        }
                    } catch (InterruptedException ex) {
                        // Do nothing, probably killing playback thread on purpose
                    } finally {
                        timerSync.unlock();
                    }
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(CardMockingboard.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            Emulator.withComputer(c->c.getMotherboard().cancelSpeedRequest(this));
            System.out.println("Mockingboard playback stopped");
            if (buffer != null && buffer.isAlive()) {
                try {
                    buffer.shutdown();
                } catch (InterruptedException | ExecutionException | SoundError e) {
                    // Ignore errors during shutdown
                }
            }
        }
    }

    private void initPSG() {
        if (phasorMode) {
            chips = new PSG[4];
            chips[0] = new PSG(0x10, CLOCK_SPEED * 2, SAMPLE_RATE, "AY1", 8);
            chips[1] = new PSG(0x80, CLOCK_SPEED * 2, SAMPLE_RATE, "AY2", 8);
            chips[2] = new PSG(0x10, CLOCK_SPEED * 2, SAMPLE_RATE, "AY3", 16);
            chips[3] = new PSG(0x80, CLOCK_SPEED * 2, SAMPLE_RATE, "AY4", 16);
        } else {
            chips = new PSG[2];
            chips[0] = new PSG(0, CLOCK_SPEED, SAMPLE_RATE, "AY1", 255);
            chips[1] = new PSG(0x80, CLOCK_SPEED, SAMPLE_RATE, "AY2", 255);
        }
    }

    @Override
    protected void handleC8FirmwareAccess(int register, TYPE type, int value, RAMEvent e) {
        // There is no c8 rom access to emulate
    }

    // This fixes freezes when resizing the window, etc.
    @Override
    public boolean suspendWithCPU() {
        return true;
    }
}
