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

import jace.LawlessLegends;
import jace.apple2e.SoftSwitches;
import jace.apple2e.softswitch.MemorySoftSwitch;
import jace.config.ConfigurableField;
import jace.config.InvokableAction;
import jace.core.Computer;
import jace.core.Device;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.state.Stateful;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Simple implementation of joystick support that supports mouse or keyboard.
 * Actual joystick support isn't offered by Java at this moment in time
 * unfortunately.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
@Stateful
public class Joystick extends Device {

    @ConfigurableField(name = "Center Mouse", shortName = "center", description = "Moves mouse back to the center of the screen, can get annoying.")
    public boolean centerMouse = false;
    @ConfigurableField(name = "Use keyboard", shortName = "useKeys", description = "Arrow keys will control joystick instead of the mouse.")
    public boolean useKeyboard = true;
    @ConfigurableField(name = "Hog keypresses", shortName = "hog", description = "Key presses will not be sent to emulator.")
    public boolean hogKeyboard = false;
    public int port;
    @Stateful
    public int x = 0;
    @Stateful
    public int y = 0;
    private int joyX = 0;
    private int joyY = 0;
    MemorySoftSwitch xSwitch;
    MemorySoftSwitch ySwitch;

    public Joystick(int port, Computer computer) {
        super(computer);
        Stage stage = LawlessLegends.getApplication().primaryStage;
        // Register a mouse handler on the primary stage that tracks the 
        // mouse x/y position as a percentage of window width and height
        stage.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (!useKeyboard) {
                joyX = (int) (event.getX() / stage.getWidth() * 255);
                joyY = (int) (event.getY() / stage.getHeight() * 255);
            }
        });
        this.port = port;
        if (port == 0) {
            xSwitch = (MemorySoftSwitch) SoftSwitches.PDL0.getSwitch();
            ySwitch = (MemorySoftSwitch) SoftSwitches.PDL1.getSwitch();
        } else {
            xSwitch = (MemorySoftSwitch) SoftSwitches.PDL2.getSwitch();
            ySwitch = (MemorySoftSwitch) SoftSwitches.PDL3.getSwitch();
        }
    }
    public boolean leftPressed = false;
    public boolean rightPressed = false;
    public boolean upPressed = false;
    public boolean downPressed = false;

    private void readJoystick() {
        if (useKeyboard) {
            joyX = (leftPressed ? -128 : 0) + (rightPressed ? 255 : 128);
            joyY = (upPressed ? -128 : 0) + (downPressed ? 255 : 128);
        }
    }

    @Override
    protected String getDeviceName() {
        return "Joystick (port " + port + ")";
    }

    @Override
    public String getShortName() {
        return "joy" + port;
    }

    @Override
    public void tick() {
        boolean finished = true;
        if (x > 0) {
            if (--x == 0) {
                xSwitch.setState(false);
            } else {
                finished = false;
            }
        }
        if (y > 0) {
            if (--y == 0) {
                ySwitch.setState(false);
            } else {
                finished = false;
            }
        }
        if (finished) {
            setRun(false);
        }
    }

    @Override
    public void attach() {
        registerListeners();
    }

    @Override
    public void detach() {
        removeListeners();
        super.detach();
    }

    @Override
    public void reconfigure() {
        removeListeners();
        x = 0;
        y = 0;
        registerListeners();
    }

    @InvokableAction(name = "Left", category = "joystick", defaultKeyMapping = "left", notifyOnRelease = true)
    public boolean joystickLeft(boolean pressed) {
        if (!useKeyboard) {
            return false;
        }
        leftPressed = pressed;
        if (pressed) {
            rightPressed = false;
        }
        return hogKeyboard;
    }

    @InvokableAction(name = "Right", category = "joystick", defaultKeyMapping = "right", notifyOnRelease = true)
    public boolean joystickRight(boolean pressed) {
        if (!useKeyboard) {
            return false;
        }
        rightPressed = pressed;
        if (pressed) {
            leftPressed = false;
        }
        return hogKeyboard;
    }

    @InvokableAction(name = "Up", category = "joystick", defaultKeyMapping = "up", notifyOnRelease = true)
    public boolean joystickUp(boolean pressed) {
        if (!useKeyboard) {
            return false;
        }
        upPressed = pressed;
        if (pressed) {
            downPressed = false;
        }
        return hogKeyboard;
    }

    @InvokableAction(name = "Down", category = "joystick", defaultKeyMapping = "down", notifyOnRelease = true)
    public boolean joystickDown(boolean pressed) {
        if (!useKeyboard) {
            return false;
        }
        downPressed = pressed;
        if (pressed) {
            upPressed = false;
        }
        return hogKeyboard;
    }

    public void initJoystickRead(RAMEvent e) {
        readJoystick();
        xSwitch.setState(true);
        x = 10 + joyX * 11;
        ySwitch.setState(true);
        y = 10 + joyY * 11;
        e.setNewValue(computer.getVideo().getFloatingBus());
        resume();
    }

    RAMListener listener;

    private void registerListeners() {
        listener = computer.getMemory().observe(RAMEvent.TYPE.ANY, 0x0c070, 0x0c07f, this::initJoystickRead);
    }

    private void removeListeners() {
        computer.getMemory().removeListener(listener);
    }
}
