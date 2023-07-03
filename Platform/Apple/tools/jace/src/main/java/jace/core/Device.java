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

import jace.config.Reconfigurable;
import jace.state.Stateful;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Device is a very simple abstraction of any emulation component. A device
 * performs some sort of action on every clock tick, unless it is waiting for a
 * number of cycles to elapse (waitCycles > 0). A device might also be paused or
 * suspended.
 *
 * Depending on the type of device, some special work might be required to
 * attach or detach it to the active emulation (such as what should happen when
 * a card is inserted or removed from a slot?) Created on May 10, 2007, 5:46 PM
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
@Stateful
public abstract class Device implements Reconfigurable {

    protected Computer computer;
    private final Set<Device> children;
    private Device[] childrenArray = new Device[0];
    private Runnable tickHandler = this::__doTickNotRunning;

    private Device() {
        children = new CopyOnWriteArraySet<>();
    }

    public Device(Computer computer) {
        this();
        this.computer = computer;
    }

    // Number of cycles to do nothing (for cpu/video cycle accuracy)
    @Stateful
    private int waitCycles = 0;
    @Stateful
    private boolean run = false;
    @Stateful
    // Pausing a device overrides its run state, and is not reset by resuming directly
    // Therefore a caller pausing a device must unpause it directly!
    private boolean paused = false;
    @Stateful
    public boolean isAttached = false;

    public void addChildDevice(Device d) {
        if (d == null || children.contains(d) || d.equals(this)) {
            return;
        }
        children.add(d);
        d.attach();
        childrenArray = children.toArray(Device[]::new);
        updateTickHandler();
    }

    public void removeChildDevice(Device d) {
        if (d == null) {
            return;
        }
        children.remove(d);
        d.suspend();
        d.detach();
        childrenArray = children.toArray(Device[]::new);
        updateTickHandler();
    }

    public void addAllDevices(Iterable<Device> devices) {
        devices.forEach(this::addChildDevice);
    }

    public Iterable<Device> getChildren() {
        return children;
    }
    
    public void setAllDevices(Collection<Device> newDevices) {
        children.stream().filter(d-> !newDevices.contains(d)).forEach(this::removeChildDevice);
        newDevices.stream().filter(d-> !children.contains(d)).forEach(this::addChildDevice);
    }

    public boolean getRunningProperty() {
        return run;
    }

    public void addWaitCycles(int wait) {
        waitCycles += wait;
    }

    public void setWaitCycles(int wait) {
        waitCycles = wait;
    }

    private void updateTickHandler() {
        if (!isRunning() || isPaused()) {
            tickHandler = this::__doTickNotRunning;
        } else if (childrenArray.length == 0) {
            tickHandler = this::__doTickNoDevices;           
        } else {
            tickHandler = this::__doTickIsRunning;
        }
    }
    
    private void __doTickNotRunning() {
        // Do nothing
    }
    
    private void __doTickIsRunning() {
        for (Device d : childrenArray) {
            d.doTick();
        }
        if (waitCycles <= 0) {
            tick();
            return;
        }
        waitCycles--;
    }
    
    private void __doTickNoDevices() {
        if (waitCycles <= 0) {
            tick();
            return;
        }
        waitCycles--;
    }
    
    public final void doTick() {
        tickHandler.run();
    }

    public boolean isRunning() {
        return run;
    }
    
    public boolean isPaused() {
        return paused;
    }

    public final synchronized void setRun(boolean run) {
        this.run = run;
        updateTickHandler();
    }

    public final synchronized void setPaused(boolean paused) {
        this.paused = paused;
        updateTickHandler();
    }
    
    protected abstract String getDeviceName();

    @Override
    public String getName() {
        return getDeviceName();
    }

    public abstract void tick();
    
    public void whileSuspended(Runnable r) {
        if (isRunning()) {
            suspend();
            r.run();
            resume();
        } else {
            r.run();
        }        
    }
    
    public boolean suspend() {
        if (isRunning()) {
//            System.out.println(getName() + " Suspended");
//            Utility.printStackTrace();
            setRun(false);
            return true;
        }
        children.forEach(Device::suspend);
        return false;
    }

    public void resume() {
        children.forEach(Device::resume);
        if (!isRunning()) {
//            System.out.println(getName() + " Resumed");
//            Utility.printStackTrace();
            setRun(true);            
            waitCycles = 0;
        }
    }

    public void attach() {
        isAttached = true;
        children.forEach(Device::attach);
    }

    public void detach() {
        children.forEach(Device::suspend);
        children.forEach(Device::detach);
        Keyboard.unregisterAllHandlers(this);
    }
}
