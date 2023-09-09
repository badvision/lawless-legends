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

import java.util.logging.Logger;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import jace.config.ConfigurableField;

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
    
    private final String defaultDeviceName;
    private long audioDevice;
    private long audioContext;
    private ALCCapabilities audioCapabilities;
    private ALCapabilities audioLibCapabilities;
    public SoundMixer(Computer computer) {
        super(computer);
        defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
    }

    // Lots of inspiration from https://www.youtube.com/watch?v=dLrqBTeipwg
    @Override
    public void attach() {
        super.attach();
        audioDevice = ALC10.alcOpenDevice(defaultDeviceName);
        // TODO: Other attributes?
        audioContext = ALC10.alcCreateContext(audioDevice, new int[]{0});
        ALC10.alcMakeContextCurrent(audioContext);
        audioCapabilities = ALC.createCapabilities(audioDevice);
        audioLibCapabilities = AL.createCapabilities(audioCapabilities);
        if (!audioLibCapabilities.OpenAL10) {
            Logger.getLogger(SoundMixer.class.getName()).warning("OpenAL 1.0 not supported");
            detach();
        }
    }

    @Override
    public void detach() {
        ALC10.alcDestroyContext(audioContext);
        ALC10.alcCloseDevice(audioDevice);
        MUTE = true;
        super.detach();
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
        } else {
            attach();
        }
    }

    @Override
    public void tick() {
    }
}
