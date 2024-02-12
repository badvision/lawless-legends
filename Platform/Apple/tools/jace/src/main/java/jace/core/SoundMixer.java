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

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import jace.Emulator;
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
     * Making this configurable requires too much effort and not a lot of benefit
     */
    // @ConfigurableField(name = "Bits per sample", shortName = "bits")
    public static int BITS = 16;
    /**
     * Sample playback rate
     */
    @ConfigurableField(name = "Playback Rate", shortName = "freq")
    public static int RATE = 44100;
    @ConfigurableField(name = "Mute", shortName = "mute")
    public static boolean MUTE = false;

    @ConfigurableField(name = "Buffer size", shortName = "buffer")
    //public static int BUFFER_SIZE = 1024;  Ok on MacOS but choppy on Windows!
    public static int BUFFER_SIZE = 2048;

    public static boolean PLAYBACK_ENABLED = false;
    // Innocent until proven guilty by a failed initialization
    public static boolean PLAYBACK_DRIVER_DETECTED = true;
    public static boolean PLAYBACK_INITIALIZED = false;
    
    private static String defaultDeviceName;
    private static long audioDevice = -1;
    private static long audioContext = -1;
    private static ALCCapabilities audioCapabilities;
    private static ALCapabilities audioLibCapabilities;
    // In case the OpenAL implementation wants to be run in a single thread, use a single thread executor
    protected static ExecutorService soundThreadExecutor = Executors.newSingleThreadExecutor();
    public SoundMixer() {
        if (!Utility.isHeadlessMode()) {
            defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);        
        }
    }

    public static class SoundError extends Exception {
        private static final long serialVersionUID = 1L;
        public SoundError(String message) {
            super(message);
        }
    }

    public static <T> T performSoundFunction(Callable<T> operation) throws SoundError {
        return performSoundFunction(operation, false);
    }

    public static <T> T performSoundFunction(Callable<T> operation, boolean ignoreError) throws SoundError {
        Future<T> result = soundThreadExecutor.submit(operation);
            try {
                if (!ignoreError) {
                    Future<Integer> error = soundThreadExecutor.submit(AL10::alGetError);
                    int err;
                    err = error.get();
                    if (err != AL10.AL_NO_ERROR) {
                        throw new SoundError(AL10.alGetString(err));
                    }
                }
                return result.get();
            } catch (ExecutionException e) {
                System.out.println("Error when executing sound action: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                // Do nothing: sound is probably being reset
            }
        return null;
    }

    public static void performSoundOperation(Runnable operation) throws SoundError {        
        performSoundOperation(operation, false);
    }

    public static void performSoundOperation(Runnable operation, boolean ignoreError) throws SoundError {
        performSoundFunction(()->{
            operation.run();
            return null;
        }, ignoreError);
    }

    public static void performSoundOperationAsync(Runnable operation) {
        soundThreadExecutor.submit(operation);
    }

    protected static void initSound() {
        if (Utility.isHeadlessMode()) {
            return;
        }
        try {
            performSoundOperation(()->{
                if (!PLAYBACK_INITIALIZED) {                
                    audioDevice = ALC10.alcOpenDevice(defaultDeviceName);
                    audioContext = ALC10.alcCreateContext(audioDevice, new int[]{0});
                    ALC10.alcMakeContextCurrent(audioContext);
                    audioCapabilities = ALC.createCapabilities(audioDevice);
                    audioLibCapabilities = AL.createCapabilities(audioCapabilities);
                    if (!audioLibCapabilities.OpenAL10) {
                        PLAYBACK_DRIVER_DETECTED = false;
                        Logger.getLogger(SoundMixer.class.getName()).warning("OpenAL 1.0 not supported");
                        Emulator.withComputer(c->c.mixer.detach());
                    }
                    PLAYBACK_INITIALIZED = true;
                } else {
                    ALC10.alcMakeContextCurrent(audioContext);
                }
            });
        } catch (SoundError e) {
            PLAYBACK_DRIVER_DETECTED = false;
            Logger.getLogger(SoundMixer.class.getName()).warning("Error when initializing sound: " + e.getMessage());
            Emulator.withComputer(c->c.mixer.detach());
        }
    }

    // Lots of inspiration from https://www.youtube.com/watch?v=dLrqBTeipwg
    @Override
    public void attach() {
        if (Utility.isHeadlessMode()) {
            return;
        }
        if (!PLAYBACK_DRIVER_DETECTED) {
            Logger.getLogger(SoundMixer.class.getName()).warning("Sound driver not detected");
            return;
        }
        super.attach();
        initSound();
        PLAYBACK_ENABLED = true;
    }

    private static List<SoundBuffer> buffers = Collections.synchronizedList(new ArrayList<>());
    public static SoundBuffer createBuffer(boolean stereo) throws InterruptedException, ExecutionException, SoundError {
        if (!PLAYBACK_ENABLED) {
            System.err.println("Sound playback not enabled, buffer not created.");
            return null;
        }
        SoundBuffer buffer = new SoundBuffer(stereo);
        buffers.add(buffer);
        return buffer;
    }

    public static class SoundBuffer {
        public static int MAX_BUFFER_ID;
        private ShortBuffer currentBuffer;
        private ShortBuffer alternateBuffer;
        private int audioFormat;
        private int currentBufferId;
        private int alternateBufferId;
        private int sourceId;
        private boolean isAlive;
        private int buffersGenerated = 0;
        
        public SoundBuffer(boolean stereo) throws InterruptedException, ExecutionException, SoundError {
            initSound();
            currentBuffer = BufferUtils.createShortBuffer(BUFFER_SIZE * (stereo ? 2 : 1));
            alternateBuffer = BufferUtils.createShortBuffer(BUFFER_SIZE * (stereo ? 2 : 1));
            try {
                currentBufferId = performSoundFunction(AL10::alGenBuffers);
                alternateBufferId = performSoundFunction(AL10::alGenBuffers);
                boolean hasSource = false;
                while (!hasSource) {
                    sourceId = performSoundFunction(AL10::alGenSources);
                    hasSource = performSoundFunction(()->AL10.alIsSource(sourceId));
                }
            } catch (SoundError e) {
                Logger.getLogger(SoundMixer.class.getName()).warning("Error when creating sound buffer: " + e.getMessage());
                Thread.dumpStack();
                shutdown();
                throw e;
            }
            audioFormat = stereo ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
            isAlive = true;
        }

        public boolean isAlive() {
            return isAlive;
        }

        /* If stereo, call this once for left and then again for right sample */
        public void playSample(short sample) throws InterruptedException, ExecutionException, SoundError {
            if (!isAlive) {
                return;
            }
            if (!currentBuffer.hasRemaining()) {
                this.flush();
            }
            currentBuffer.put(sample);
        }

        public void shutdown() throws InterruptedException, ExecutionException, SoundError {            
            if (!isAlive) {
                return;
            }
            isAlive = false;
            
            try {
                performSoundOperation(()->{if (AL10.alIsSource(sourceId)) AL10.alSourceStop(sourceId);});
            } finally {
                try {
                    performSoundOperation(()->{if (AL10.alIsSource(sourceId)) AL10.alDeleteSources(sourceId);});
                } finally {
                    sourceId = -1;
                    try {
                        performSoundOperation(()->{if (AL10.alIsBuffer(alternateBufferId)) AL10.alDeleteBuffers(alternateBufferId);});
                    } finally {
                        alternateBufferId = -1;
                        try {
                            performSoundOperation(()->{if (AL10.alIsBuffer(currentBufferId)) AL10.alDeleteBuffers(currentBufferId);});
                        } finally {
                            currentBufferId = -1;
                            buffers.remove(this);
                        }
                    }
                }
            }
        }

        public void flush() throws SoundError {
            buffersGenerated++;
            currentBuffer.flip();
            if (buffersGenerated > 2) {
                int[] unqueueBuffers = new int[]{currentBufferId};
                performSoundOperation(()->{
                    int buffersProcessed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
                    while (buffersProcessed < 1) {
                        Thread.onSpinWait();
                        buffersProcessed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
                    }
                });
                if (!isAlive) {
                    return;
                }
                performSoundOperation(()->{
                    AL10.alSourceUnqueueBuffers(sourceId, unqueueBuffers);
                });
            }
            if (!isAlive) {
                return;
            }
            performSoundOperation(()->AL10.alBufferData(currentBufferId, audioFormat, currentBuffer, RATE));
            if (!isAlive) {
                return;
            }
            performSoundOperation(()->AL10.alSourceQueueBuffers(sourceId, currentBufferId));
            performSoundOperationAsync(()->{
                if (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(sourceId);
                }
            });

            // Swap AL buffers
            int tempId = currentBufferId;
            currentBufferId = alternateBufferId;
            alternateBufferId = tempId;
            // Swap Java buffers
            ShortBuffer tempBuffer = currentBuffer;
            currentBuffer = alternateBuffer;
            alternateBuffer = tempBuffer;                
        }
    }

    public int getActiveBuffers() {
        return buffers.size();
    }

    @Override
    public void detach() {
        if (!PLAYBACK_ENABLED) {
            return;
        }   
        MUTE = true;
        PLAYBACK_ENABLED = false;
        
        while (!buffers.isEmpty()) {
            SoundBuffer buffer = buffers.remove(0);
            try {
                buffer.shutdown();
            } catch (InterruptedException | ExecutionException | SoundError e) {
                Logger.getLogger(SoundMixer.class.getName()).warning("Error when detaching sound mixer: " + e.getMessage());
            }
        }
        buffers.clear();
        PLAYBACK_INITIALIZED = false;
        try {
            performSoundOperation(()->ALC10.alcDestroyContext(audioContext), true);
            performSoundOperation(()->ALC10.alcCloseDevice(audioDevice), true);
        } catch (SoundError e) {
            // Shouldn't throw but have to catch anyway
        }
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
        PLAYBACK_ENABLED = PLAYBACK_DRIVER_DETECTED && !MUTE;
        if (PLAYBACK_ENABLED) {
            attach();
        } else {
            detach();
        }
    }

    @Override
    public void tick() {
    }
}
