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

import jace.config.ConfigurableField;

/**
 * A timed device is a device which executes so many ticks in a given time interval. This is the core of the emulator
 * timing mechanics.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public abstract class TimedDevice extends Device {

    /**
     * Creates a new instance of TimedDevice
     *
     * @param computer
     */
    public TimedDevice() {
        super();
        setSpeedInHz(cyclesPerSecond);
    }
    @ConfigurableField(name = "Speed", description = "(Percentage)")
    public int speedRatio = 100;
    private long cyclesPerSecond = defaultCyclesPerSecond();
    @ConfigurableField(name = "Max speed")
    public boolean maxspeed = false;

    @Override
    public abstract void tick();
    private static final double NANOS_PER_SECOND = 1000000000.0;
    // current cycle within the period
    private int cycleTimer = 0;
    // The actual worker that the device runs as
    public Thread worker;
    public static int TEMP_SPEED_MAX_DURATION = 1000000;
    private int tempSpeedDuration = 0;
    public boolean hasStopped = true;

    @Override
    public boolean suspend() {
        disableTempMaxSpeed();
        boolean result = super.suspend();
        Thread w = worker;
        if (w != null && w.isAlive()) {
            try {
                w.interrupt();
                w.join(1000);
            } catch (InterruptedException ex) {
            }
        }
        worker = null;
        return result;
    }

    @Override
    /* We really don't want to suspect the worker thread if we're running in it.
     * The goal for suspending the thread is to prevent any concurrent activity
     * affecting the emulator state.  However, if we're already in the worker
     * thread, then we're already blocking the execution of the emulator, so
     * we don't need to suspend it.
     */
    public void whileSuspended(Runnable r) {
        if (isDeviceThread()) {
            r.run();            
        } else {
            super.whileSuspended(r);
        }
    }

    @Override
    public <T> T whileSuspended(java.util.function.Supplier<T> r, T defaultValue) {
        if (isDeviceThread()) {
            return r.get();
        } else {
            return super.whileSuspended(r, defaultValue);
        }
    }

    public boolean pause() {
        if (!isRunning()) {
            return false;
        }
        super.setPaused(true);
        try {
            // KLUDGE: Sleeping to wait for worker thread to hit paused state.  We might be inside the worker (?)
            if (!isDeviceThread()) {
                Thread.sleep(10);
            }
        } catch (InterruptedException ex) {
        }
        return true;
    }

    @Override
    public void setPaused(boolean paused) {
        if (!isPaused() && paused) {
            pause();
        } else {
            super.setPaused(paused);
        }
    }

    public boolean isDeviceThread() {
        return worker != null && worker.isAlive() && Thread.currentThread() == worker;
    }

    /**
     * This is used in unit tests where we want the device
     * to act like it is resumed, but not actual free-running.
     * This allows tests to step manually to check behaviors, etc.
     */
    public void resumeInThread() {
        super.resume();
        setPaused(false);
    }
    
    @Override
    public void resume() {
        super.resume();
        setPaused(false);
        if (worker != null && worker.isAlive()) {
            return;
        }
        Thread newWorker = new Thread(() -> {
            // System.out.println("Worker thread for " + getDeviceName() + " starting");
            while (isRunning()) {
                hasStopped = false;
                doTick();
                while (isPaused() && isRunning()) {
                    hasStopped = true;
                    Thread.onSpinWait();
                }
                if (!maxspeed) {
                    resync();
                }
            }
            hasStopped = true;
            // System.out.println("Worker thread for " + getDeviceName() + " stopped");
        });
        this.worker = newWorker;
        newWorker.setDaemon(false);
        newWorker.setPriority(Thread.MAX_PRIORITY);
        newWorker.setName("Timed device " + getDeviceName() + " worker");
        newWorker.start();
    }
    long nanosPerInterval; // How long to wait between pauses
    long cyclesPerInterval; // How many cycles to wait until a pause interval
    long nextSync; // When was the last pause?

    public final int getSpeedRatio() {
        return speedRatio;
    }

    public final void setMaxSpeed(boolean enabled) {
        maxspeed = enabled;
        if (!enabled) {
            disableTempMaxSpeed();
        }
    }

    public final boolean isMaxSpeed() {
        return maxspeed;
    }

    public final long getSpeedInHz() {
        return cyclesPerInterval * 100L;
    }

    public final void setSpeedInHz(long cyclesPerSecond) {
//        System.out.println("Raw set speed for " + getName() + " to " + cyclesPerSecond + "hz");
        speedRatio = (int) Math.round(cyclesPerSecond * 100.0 / defaultCyclesPerSecond());
        cyclesPerInterval = cyclesPerSecond / 100L;
        nanosPerInterval = (long) (cyclesPerInterval * NANOS_PER_SECOND / cyclesPerSecond);
//        System.out.println("Will pause " + nanosPerInterval + " nanos every " + cyclesPerInterval + " cycles");
        cycleTimer = 0;
        resetSyncTimer();
    }

    public final void setSpeedInPercentage(int ratio) {
//        System.out.println("Setting " + getName() + " speed ratio to " + speedRatio);
        cyclesPerSecond = defaultCyclesPerSecond() * ratio / 100;
        if (cyclesPerSecond == 0) {
            cyclesPerSecond = defaultCyclesPerSecond();
        }
        setSpeedInHz(cyclesPerSecond);
    }

    public final void resetSyncTimer() {
        nextSync = System.nanoTime() + nanosPerInterval;
        cycleTimer = 0;
    }

    public void enableTempMaxSpeed() {
        tempSpeedDuration = TEMP_SPEED_MAX_DURATION;
    }

    public void disableTempMaxSpeed() {
        tempSpeedDuration = 0;
        resetSyncTimer();
    }

    protected void resync() {
        if (++cycleTimer >= cyclesPerInterval) {
            if (tempSpeedDuration > 0 || isMaxSpeed()) {
                tempSpeedDuration -= cyclesPerInterval;
                resetSyncTimer();
                return;
            }
            long now = System.nanoTime();
            if (now < nextSync) {
                cycleTimer = 0;
                long currentSyncDiff = nextSync - now;
                // Don't bother resynchronizing unless we're off by 10ms
                if (currentSyncDiff > 10000000L) {
                    try {
//                        System.out.println("Sleeping for " + currentSyncDiff / 1000000 + " milliseconds");
                        Thread.sleep(currentSyncDiff / 1000000L, (int) (currentSyncDiff % 1000000L));
                    } catch (InterruptedException ex) {
                        System.err.println(getDeviceName() + " was trying to sleep for " + (currentSyncDiff / 1000000) + " millis but was woken up");
//                        Logger.getLogger(TimedDevice.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
//                    System.out.println("Sleeping for " + currentSyncDiff + " nanoseconds");
//                    LockSupport.parkNanos(currentSyncDiff);
                }
            }
            nextSync += nanosPerInterval;
        }
    }

    @Override
    public void reconfigure() {
    }

    public abstract long defaultCyclesPerSecond();
}
