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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jace.config.ConfigurableField;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.FloatControl;

/**
 * Manages sound resources used by various audio devices (such as speaker and
 * mockingboard cards.) The plumbing is managed in this class so that the
 * consumers do not have to do a lot of work to manage mixer lines or deal with
 * how to reuse active lines if needed. It is possible that this class might be
 * used to manage volume in the future, but that remains to be seen.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class SoundMixer extends Device {

    /**
     * Bits per sample
     */
    @ConfigurableField(name = "Bits per sample", shortName = "bits")
    public static int BITS = 16;
    /**
     * Sample playback rate
     */
    @ConfigurableField(name = "Playback Rate", shortName = "freq")
    public static int RATE = 48000;
    @ConfigurableField(name = "Mute", shortName = "mute")
    public static boolean MUTE = false;
    
    public SoundMixer(Computer computer) {
        super(computer);
    }

    /**
     * Get a javafx media sourcedataline for stereo 44.1KHz 16-bit signed PCM data.
     * Confirm the line is open before using it.
     * 
     * @return 
     */
    public SourceDataLine getLine() {
        if (MUTE) {
            return null;
        }
        
        SourceDataLine line = null;
        try {
            // WAV is a little endian format, so it makes sense to stick with that.
            AudioFormat format = new AudioFormat(RATE, BITS, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
//            Logger.getLogger(getClass().getName()).log(Level.INFO, "Obtained source data line: %s, buffer size %d".formatted(line.getFormat(), line.getBufferSize()));
        } catch (LineUnavailableException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error getting sound line: {0}", e.getMessage());
        }
        return line;
    }    

    @Override
    public String getDeviceName() {
        return "Sound Output";
    }

    @Override
    public String getShortName() {
        return "mixer";
    }

    @Override
    public synchronized void reconfigure() {
        if (MUTE) {
            detach();
        }
    }

    public byte randomByte() {
        return (byte) (Math.random() * 256);
    }

    @Override
    public void tick() {
    }
}
