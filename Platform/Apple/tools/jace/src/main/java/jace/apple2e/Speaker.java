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
package jace.apple2e;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.SourceDataLine;

import jace.Emulator;
import jace.LawlessLegends;
import jace.config.ConfigurableField;
import jace.config.InvokableAction;
import jace.core.Computer;
import jace.core.Motherboard;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.SoundGeneratorDevice;
import jace.core.SoundMixer;
import javafx.stage.FileChooser;

/**
 * Apple // Speaker Emulation Created on May 9, 2007, 9:55 PM
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class Speaker extends SoundGeneratorDevice {

    static boolean fileOutputActive = false;
    static OutputStream out;
    
    @ConfigurableField(category = "sound", name = "1mhz timing", description = "Force speaker output to 1mhz?")
    public static boolean force1mhz = true;

    @InvokableAction(category = "sound", name = "Record sound", description="Toggles recording (saving) sound output to a file", defaultKeyMapping = "ctrl+shift+w")
    public static void toggleFileOutput() {
        if (fileOutputActive) {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
            }
            out = null;
            fileOutputActive = false;
        } else {
            FileChooser fileChooser = new FileChooser();
            File f = fileChooser.showSaveDialog(LawlessLegends.getApplication().primaryStage);
            if (f == null) {
                return;
            }
            try {
                out = new FileOutputStream(f);
                fileOutputActive = true;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Counter tracks the number of cycles between sampling
     */
    private double counter = 0;
    /**
     * Level is the number of cycles the speaker has been on
     */
    private int level = 0;
    /**
     * Idle cycles counts the number of cycles the speaker has not been changed
     * (used to deactivate sound when not in use)
     */
    private int idleCycles = 0;
    /**
     * Number of samples in buffer
     */
    static int BUFFER_SIZE = (int) (SoundMixer.RATE * 0.4);
    /**
     * Playback volume (should be < 1423)
     */
    @ConfigurableField(name = "Speaker Volume", shortName = "vol", description = "Should be under 1400")
    public static int VOLUME = 600;
    /**
     * Number of idle cycles until speaker playback is deactivated
     */
    @ConfigurableField(name = "Idle cycles before sleep", shortName = "idle")
    public static int MAX_IDLE_CYCLES = 2000000;
    /**
     * Java sound output
     */
    private SourceDataLine sdl;
    /**
     * Manifestation of the apple speaker softswitch
     */
    private boolean speakerBit = false;
    //
    /**
     * Locking semaphore to prevent race conditions when working with buffer or
     * related variables
     */
    private final Object bufferLock = new Object();
    /**
     * Double-buffer used for playing processed sound -- as one is played the
     * other fills up.
     */
    private byte[] primaryBuffer;
    private byte[] secondaryBuffer;
    private int bufferPos = 0;
    private Timer playbackTimer;
    private double TICKS_PER_SAMPLE = ((double) Motherboard.DEFAULT_SPEED) / SoundMixer.RATE;
    private double TICKS_PER_SAMPLE_FLOOR = Math.floor(TICKS_PER_SAMPLE);
    private RAMListener listener = null;

    /**
     * Creates a new instance of Speaker
     *
     * @param computer
     */
    public Speaker(Computer computer) {
        super(computer);
    }

    /**
     * Suspend playback of sound
     *
     * @return
     */
    @Override
    public boolean suspend() {
        boolean result = super.suspend();
        if (playbackTimer != null) {
            playbackTimer.cancel();
            playbackTimer = null;
        }
        speakerBit = false;
        if (sdl != null && sdl.isOpen()) {
            sdl.stop();
            sdl.close();
        }
        sdl = null;

        return result;
    }

    /**
     * Start or resume playback of sound
     */
    @Override
    public void resume() {
        if (sdl == null || !sdl.isOpen()) {
            sdl = computer.mixer.getLine();
            if (sdl != null) {
                sdl.start();
                counter = 0;
                idleCycles = 0;
                level = 0;
                bufferPos = 0;
            } else {
                Logger.getLogger(getClass().getName()).severe("Unable to get audio line for speaker!");
                detach();
                return;
            }
        }

        if (force1mhz) {
            TICKS_PER_SAMPLE = ((double) Motherboard.DEFAULT_SPEED) / SoundMixer.RATE;
        } else {
            TICKS_PER_SAMPLE = Emulator.withComputer(c-> ((double) c.getMotherboard().getSpeedInHz()) / SoundMixer.RATE, 0.0);
        }
        TICKS_PER_SAMPLE_FLOOR = Math.floor(TICKS_PER_SAMPLE);

        setRun(true);
    }

    public void playCurrentBuffer() {
        byte[] buffer;
        int len;
        synchronized (bufferLock) {
            len = bufferPos;
            buffer = primaryBuffer;
            primaryBuffer = secondaryBuffer;
            bufferPos = 0;
        }
        secondaryBuffer = buffer;
        if (sdl != null && len > 0) {
            sdl.write(buffer, 0, len);
        }
    }

    /**
     * Reset idle counter whenever sound playback occurs
     */
    public void resetIdle() {
        idleCycles = 0;
        if (!isRunning()) {
            resume();
        }
    }

    /**
     * Motherboard cycle tick Every 23 ticks a sample will be added to the
     * buffer If the buffer is full, this will block until there is room in the
     * buffer, thus keeping the emulation in sync with the sound
     */
    @Override
    public void tick() {
        if (idleCycles++ >= MAX_IDLE_CYCLES) {
            suspend();
        }
        if (speakerBit) {
            level++;
        }
        counter += 1.0d;
        if (counter >= TICKS_PER_SAMPLE) {
            playSample(level * VOLUME);

            // Set level back to 0
            level = 0;
            // Set counter to 0
            counter -= TICKS_PER_SAMPLE_FLOOR;
        }
    }

    private void toggleSpeaker(RAMEvent e) {
        if (e.getType() == RAMEvent.TYPE.WRITE) {
            level += 2;
        }
        speakerBit = !speakerBit;
        resetIdle();
    }
    
    private void playSample(int sample) {
        if (sdl == null || !sdl.isOpen()) {
            resume();
        }
        int bytes = SoundMixer.BITS >> 3;

        // Prepare sound output in little endian format
        for (int i = 0; i < bytes; i++) {
            primaryBuffer[i] = primaryBuffer[i+bytes] = (byte) (sample & 0x0ff);
            sample >>= 8;
        }            
        sdl.write(primaryBuffer, 0, bytes*2);
        if (fileOutputActive) {
            try {
                out.write(primaryBuffer, 0, bytes*2);
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error recording sound", ex);
                toggleFileOutput();
            }
        }
        
    }

    /**
     * Add a memory event listener for C03x for capturing speaker events
     */
    private void configureListener() {
        listener = computer.getMemory().observe(RAMEvent.TYPE.ANY, 0x0c030, 0x0c03f, this::toggleSpeaker);
    }

    private void removeListener() {
        computer.getMemory().removeListener(listener);
    }

    /**
     * Returns "Speaker"
     *
     * @return "Speaker"
     */
    @Override
    protected String getDeviceName() {
        return "Speaker";
    }

    @Override
    public String getShortName() {
        return "spk";
    }

    @Override
    public final void reconfigure() {
        super.reconfigure();
        
        if (primaryBuffer != null && secondaryBuffer != null) {
            return;
        }
        BUFFER_SIZE = 20000 * (SoundMixer.BITS >> 3);
        primaryBuffer = new byte[BUFFER_SIZE];
        secondaryBuffer = new byte[BUFFER_SIZE];
    }

    @Override
    public void attach() {
        configureListener();
        resume();
    }

    @Override
    public void detach() {
        removeListener();
        suspend();
        super.detach();
    }
}
