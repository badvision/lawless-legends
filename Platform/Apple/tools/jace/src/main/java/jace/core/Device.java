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

import jace.state.Stateful;
import jace.config.Reconfigurable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

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
    private MutableList<Device> children;

    private Device() {
        // TODO: Previously this was synchronized -- confirm this is safe to leave unsynchronized
        children = Lists.mutable.<Device>empty();
    }

    public Device(Computer computer) {
        this();
        this.computer = computer;
    }

    // Number of cycles to do nothing (for cpu/video cycle accuracy)
    @Stateful
    private int waitCycles = 0;
    @Stateful
    private boolean run = true;
    @Stateful
    public boolean isPaused = false;
    @Stateful
    public boolean isAttached = false;

    public void addChildDevice(Device d) {
        children.add(d);
        if (isAttached) {
            d.attach();
        }
    }

    public void removeChildDevice(Device d) {
        children.remove(d);
        d.suspend();
        if (isAttached) {
            d.detach();
        }
    }

    public void addAllDevices(Iterable<Device> devices) {
        devices.forEach(this::addChildDevice);
    }

    public Iterable<Device> getChildren() {
        return children.asUnmodifiable();
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

    public void doTick() {
        if (run) {
            children.forEach(Device::tick);
            if (waitCycles <= 0) {
                tick();
                return;
            }
            waitCycles--;
        }
    }

    public boolean isRunning() {
        return run;
    }

    public synchronized void setRun(boolean run) {
//        System.out.println(Thread.currentThread().getName() + (run ? " resuming " : " suspending ")+ getDeviceName());
        isPaused = false;
        this.run = run;
    }

    protected abstract String getDeviceName();

    @Override
    public String getName() {
        return getDeviceName();
    }

    public abstract void tick();

    public boolean suspend() {
        if (isRunning()) {
            setRun(false);
            return true;
        }
        children.forEach(Device::suspend);
        return false;
    }

    public void resume() {
        setRun(true);
        waitCycles = 0;
        children.forEach(Device::resume);
    }

    public void attach() {
        isAttached = true;
        children.forEach(Device::attach);
    }

    public void detach() {
        Keyboard.unregisterAllHandlers(this);
        children.forEach(Device::suspend);
        children.forEach(Device::detach);
    }
}
