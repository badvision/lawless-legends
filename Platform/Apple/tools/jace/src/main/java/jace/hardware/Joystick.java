/** 
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.hardware;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;

import org.lwjgl.glfw.GLFW;

import jace.Emulator;
import jace.LawlessLegends;
import jace.apple2e.SoftSwitches;
import jace.apple2e.softswitch.MemorySoftSwitch;
import jace.config.ConfigurableField;
import jace.config.DynamicSelection;
import jace.config.InvokableAction;
import jace.core.Computer;
import jace.core.Device;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.TimedDevice;
import jace.state.Stateful;
import javafx.application.Platform;
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
    static {
        Platform.runLater(()->{
            GLFW.glfwInit();
        });
    }
    
    @ConfigurableField(name = "Center Mouse", shortName = "center", description = "Moves mouse back to the center of the screen, can get annoying.")
    public boolean centerMouse = false;
    @ConfigurableField(name = "Use keyboard", shortName = "useKeys", description = "Arrow keys will control joystick instead of the mouse.")
    public boolean useKeyboard = false;
    @ConfigurableField(name = "Hog keypresses", shortName = "hog", description = "Key presses will not be sent to emulator.")
    public boolean hogKeyboard = false;
    @ConfigurableField(name = "Controller", shortName = "glfwController", description = "Physical game controller")
    public DynamicSelection<String> glfwController = new DynamicSelection<>(null) {
        @Override
        public boolean allowNull() {
            return true;
        }

        @Override
        public LinkedHashMap<String, String> getSelections() {
            // Get list of joysticks from GLFW
            LinkedHashMap<String, String> selections = new LinkedHashMap<>();
            selections.put("", "***Empty***");
            for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
                if (GLFW.glfwJoystickPresent(i)) {
                    selections.put(GLFW.glfwGetJoystickName(i), GLFW.glfwGetJoystickName(i));
                }
            }
            return selections;
        }
    };
    @ConfigurableField(name = "X Axis", shortName = "xaxis", description = "Physical game controller X Axis")
    public int xaxis = GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
    @ConfigurableField(name = "Y Axis", shortName = "yaxis", description = "Physical game controller Y Axis")
    public int yaxis = GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
    @ConfigurableField(name = "Button 0", shortName = "buttonA", description = "Physical game controller A button")
    public int button0 = GLFW.GLFW_GAMEPAD_BUTTON_A;
    @ConfigurableField(name = "Button 1", shortName = "buttonB", description = "Physical game controller B button")
    public int button1 = GLFW.GLFW_GAMEPAD_BUTTON_B;
    @ConfigurableField(name = "Prefer D-PAD", shortName = "dpad", description = "Physical game controller enable D-PAD")
    public boolean useDPad = false;
    @ConfigurableField(name = "Timer resolution", shortName = "timerres", description = "How many ticks until we poll the buttons again?")
    public static long TIMER_RESOLUTION = TimedDevice.NTSC_1MHZ / 15; // 15FPS resolution reads when joystick is not used
    @ConfigurableField(name = "Dead Zone", shortName = "deadZone", description = "Dead zone for joystick (0-1)")
    public static float deadZone = 0.1f;

    Integer controllerNumber = null;

    public Integer getControllerNum() {
        if (controllerNumber != null) {
            return controllerNumber >= 0  && GLFW.glfwJoystickPresent(controllerNumber) ? controllerNumber : null;
        }
        String controllerName = glfwController.getValue();
        if (controllerName == null || controllerName.isEmpty()) {
            return null;
        }
        for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i) && controllerName.equals(GLFW.glfwGetJoystickName(i))) {
                controllerNumber = i;
                // Let's list out all the buttons and axis data
                Platform.runLater(()->{
                    FloatBuffer axes = GLFW.glfwGetJoystickAxes(controllerNumber);
                    ByteBuffer buttons = GLFW.glfwGetJoystickButtons(controllerNumber);
                    System.out.println("Controller " + controllerName + " has " + axes.capacity() + " axes and " + buttons.capacity() + " buttons");
                });
                return i;
            }
        }
        return null;
    }
    private boolean selectedPhysicalController() {
        return getControllerNum() != null;
    }
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
        super();
        if (LawlessLegends.getApplication() == null) {
            return;
        }
        Stage stage = LawlessLegends.getApplication().primaryStage;
        // Register a mouse handler on the primary stage that tracks the 
        // mouse x/y position as a percentage of window width and height
        stage.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (!useKeyboard && !selectedPhysicalController()) {
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
        ticksSinceLastRead = 0;
        if (useKeyboard) {
            joyX = (leftPressed ? -128 : 0) + (rightPressed ? 255 : 128);
            joyY = (upPressed ? -128 : 0) + (downPressed ? 255 : 128);
        } else if (selectedPhysicalController()) {
            Platform.runLater(()->{
                Integer controllerNum = getControllerNum();

                if (controllerNum != null) {
                    FloatBuffer axes = GLFW.glfwGetJoystickAxes(controllerNumber);
                    ByteBuffer buttons = GLFW.glfwGetJoystickButtons(controllerNumber);
                    float x = xaxis >= 0 && xaxis < axes.capacity() ? axes.get(xaxis) : -0.5f;
                    float y = yaxis >= 0 && yaxis < axes.capacity() ? axes.get(yaxis) : 0.5f;

                    if (useDPad) {
                        // ByteBuffer buttons = GLFW.glfwGetJoystickButtons(controllerNum);
                        if (buttons.get(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT) != 0) {
                            x = -1;
                        } else if (buttons.get(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) != 0) {
                            x = 1;
                        } else {
                            x = 0;
                        }

                        if (buttons.get(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP) != 0) {
                            y = -1;
                        } else if (buttons.get(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN) != 0) {
                            y = 1;
                        } else {
                            y = 0;
                        }
                    }
                    if (Math.abs(x) < deadZone) {
                        x = 0;
                    }
                    if (Math.abs(y) < deadZone) {
                        y = 0;
                    }
                    joyX = (int) (x * 128.0 + 128.0);
                    joyY = (int) (y * 128.0 + 128.0);

                    readButtons();
                }
            });
        }
    }

    private void readButtons() {
        if (selectedPhysicalController()) {
            Integer controllerNum = getControllerNum();
            if (controllerNum != null) {
                ByteBuffer buttons = GLFW.glfwGetJoystickButtons(controllerNumber);
                byte b0 = button0 >=0 && button0 < buttons.capacity() ? buttons.get(button0) : 0;
                byte b1 = button1 >=0 && button1 < buttons.capacity() ? buttons.get(button1) : 0;
                SoftSwitches.PB0.getSwitch().setState(b0 != 0);
                SoftSwitches.PB1.getSwitch().setState(b1 != 0);
            }
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

    int ticksSinceLastRead = Integer.MAX_VALUE;

    @Override
    public void tick() {
        ticksSinceLastRead++;
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
        if (selectedPhysicalController()) {
            if (finished && ticksSinceLastRead >= 1000000) {
                setRun(false);
            } else if (ticksSinceLastRead % TIMER_RESOLUTION == 0) {
                // Read joystick buttons ~60 times a second as long as joystick is actively in use
                Integer controllerNum = getControllerNum();
                if (controllerNum != null) {
                    Platform.runLater(this::readButtons);
                }
            }
        } else if (finished) {
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
        controllerNumber = null;
    }

    @InvokableAction(name = "Left", category = "joystick", defaultKeyMapping = "left", notifyOnRelease = true)
    public boolean joystickLeft(boolean pressed) {
        if (!isAttached || !useKeyboard) {
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
        if (!isAttached || !useKeyboard) {
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
        if (!isAttached || !useKeyboard) {
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
        if (!isAttached || !useKeyboard) {
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
        Emulator.withVideo(v->e.setNewValue(v.getFloatingBus()));
        resume();
    }

    RAMListener listener;

    private void registerListeners() {
        listener = getMemory().observe("Joystick I/O", RAMEvent.TYPE.ANY, 0x0c070, 0x0c07f, this::initJoystickRead);
    }

    private void removeListeners() {
        getMemory().removeListener(listener);
    }
}
