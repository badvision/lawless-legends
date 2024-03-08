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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
import jace.core.Keyboard;
import jace.core.RAMEvent;
import jace.core.RAMListener;
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
            // Load joystick mappings from resources
            // First read the file into a ByteBuffer
            try (InputStream inputStream = Joystick.class.getResourceAsStream("/jace/data/gamecontrollerdb.txt")) {
                // Throw it into a string
                String mappings = new String(inputStream.readAllBytes());
                parseGameControllerDB(mappings);
            } catch (Exception e) {
                System.err.println("Failed to load joystick mappings; error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    static public class ControllerMapping {
        public String name;
        public String guid;
        public String platform;
        public int button0;
        public int button1;
        public boolean xinvert;
        public int xaxis;
        public boolean yinvert;
        public int yaxis;
        public int up;
        public int down;
        public int left;
        public int right;
    }

    static Map<String, ControllerMapping> controllerMappings = new HashMap<>();
    static void parseGameControllerDB(String mappings) {
        // File format: 
        // Any line starting with a # or empty is a comment
        // Format is GUID, name, mappings
        // Mappings are a comma-separated list of buttons, axes, and hats with a : delimiter
        // Buttons are B<index>, axes are A<index>, hats are H<index>

        // Read into a map of GUID to mappings
        String[] lines = mappings.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 3) {
                continue;
            }
            String guid = parts[0].trim();
            String name = parts[1].trim();
            ControllerMapping controller = new ControllerMapping();
            controller.guid = guid;
            controller.name = name;
            controllerMappings.put(guid, controller);
            // Split the mapping into parts
            for (int i = 2; i < parts.length; i++) {
                String[] mappingParts = parts[i].split(":");
                if (mappingParts.length < 2) {
                    continue;
                }
                String target = mappingParts[0].trim();
                String source = mappingParts[1].trim();
                boolean inverted = source.endsWith("~");
                if (inverted) {
                    source = source.substring(0, source.length() - 1);
                }
                boolean isAxis = source.charAt(0) == 'a';
                boolean isButton = source.charAt(0) == 'b';
                boolean isHat = source.charAt(0) == 'h';
                boolean isNAN = !isAxis && !isButton && !isHat;
                int index = isNAN ? -1 : Integer.parseInt(source.substring(isHat ? 3 : 1));
                switch (target) {
                    case "a":
                        controller.button0 = isButton ? index : 1;
                        break;
                    case "b":
                        controller.button1 = isButton ? index : 2;
                        break;
                    case "leftx":
                        controller.xaxis = isAxis ? index : 0;
                        controller.xinvert = inverted;
                        break;
                    case "lefty":
                        controller.yaxis = isAxis ? index : 1;
                        controller.yinvert = inverted;
                        break;
                    case "dpup":
                        controller.up = isButton ? index : 3;
                        break;
                    case "dpdown":
                        controller.down = isButton ? index : 4;
                        break;
                    case "dpleft":
                        controller.left = isButton ? index : 5;
                        break;
                    case "dpright":
                        controller.right = isButton ? index : 6;
                        break;
                    case "platform":
                        controller.platform = source;
                        break;
                }
            }

        }

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
    public int xaxis = 0;
    @ConfigurableField(name = "Y Axis", shortName = "yaxis", description = "Physical game controller Y Axis")
    public int yaxis = 1;
    @ConfigurableField(name = "Button 0", shortName = "buttonA", description = "Physical game controller A button")
    public int button0 = 1;
    @ConfigurableField(name = "Button 1", shortName = "buttonB", description = "Physical game controller B button")
    public int button1 = 2;
    @ConfigurableField(name = "Use D-PAD", shortName = "dpad", description = "Physical game controller enable D-PAD")
    public boolean useDPad = true;
    @ConfigurableField(name = "Dead Zone", shortName = "deadZone", description = "Dead zone for joystick (0-1)")
    public static float deadZone = 0.1f;

    Integer controllerNumber = null;
    ControllerMapping controllerMapping = null;

    public Integer getControllerNum() {
        if (controllerNumber != null) {
            return controllerNumber >= 0 ? controllerNumber : null;
        }
        String controllerName = glfwController.getValue();
        if (controllerName == null || controllerName.isEmpty()) {
            return null;
        }
        for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (controllerName.equals(GLFW.glfwGetJoystickName(i)) && GLFW.glfwJoystickPresent(i)) {
                controllerNumber = i;
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

    long lastPollTime = System.currentTimeMillis();
    FloatBuffer axes;
    ByteBuffer buttons;

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
        } else if (readGLFWJoystick()) {
            float x = -0.5f;
            float y = 0.5f;
            if (controllerMapping != null) {
                x = axes.get(controllerMapping.xaxis) * (controllerMapping.xinvert ? -1.0f : 1.0f);
                y = axes.get(controllerMapping.yaxis) * (controllerMapping.yinvert ? -1.0f : 1.0f);
            } else {
                if (xaxis >= 0 && xaxis < axes.capacity()) {
                    x = axes.get(xaxis);
                }
                if (yaxis >= 0 && yaxis < axes.capacity()) {
                    y = axes.get(yaxis);
                }
            }

            if (useDPad && controllerMapping != null) {
                if (buttons.get(controllerMapping.left) != 0) {
                    x = -1;
                } else if (buttons.get(controllerMapping.right) != 0) {
                    x = 1;
                }

                if (buttons.get(controllerMapping.up) != 0) {
                    y = -1;
                } else if (buttons.get(controllerMapping.down) != 0) {
                    y = 1;
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
    }

    public static long POLLING_TIME = 15;
    public static int CALIBRATION_ITERATIONS = 15;
    private void calibrateTiming() {
        if (selectedPhysicalController()) {
            Integer controllerNum = getControllerNum();
            if (controllerNum != null) {
                long start = System.currentTimeMillis();

                Emulator.whileSuspended((c) -> {
                    for (int i = 0; i < CALIBRATION_ITERATIONS; i++) {
                        buttons = GLFW.glfwGetJoystickButtons(controllerNumber);
                        axes = GLFW.glfwGetJoystickAxes(controllerNumber);
                    }
                });
                String guid = GLFW.glfwGetJoystickGUID(controllerNumber);
                long end = System.currentTimeMillis();
                POLLING_TIME = (end - start) / CALIBRATION_ITERATIONS + 1;
                POLLING_TIME = Math.min(POLLING_TIME*2, 45);
                lastPollTime = end;
                System.out.println("Calibrated polling time to " + POLLING_TIME + "ms for joystick " + guid);
            }
        }
    }

    private boolean readGLFWJoystick() {
        if (System.currentTimeMillis() - lastPollTime >= POLLING_TIME) {
            lastPollTime = System.currentTimeMillis();
            if (selectedPhysicalController()) {
                Integer controllerNum = getControllerNum();
                if (controllerNum != null) {
                    Platform.runLater(()->{
                        buttons = GLFW.glfwGetJoystickButtons(controllerNumber);
                        axes = GLFW.glfwGetJoystickAxes(controllerNumber);
                    });
                }
            }
        }
        return axes != null && buttons != null;
    }

    private void readButtons() {
        if (readGLFWJoystick()) {
            byte b0 = 0;
            byte b1 = 0;
            if (controllerMapping != null) {
                b0 = buttons.get(controllerMapping.button0);
                b1 = buttons.get(controllerMapping.button1);
            } else if (button0 >= 0 && button0 < buttons.capacity()) {
                b0 = button0 >=0 && button0 < buttons.capacity() ? buttons.get(button0) : 0;
                b1 = button1 >=0 && button1 < buttons.capacity() ? buttons.get(button1) : 0;
            }
            SoftSwitches.PB0.getSwitch().setState(b0 != 0 || Keyboard.isOpenApplePressed);
            SoftSwitches.PB1.getSwitch().setState(b1 != 0 || Keyboard.isClosedApplePressed);
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
            ticksSinceLastRead++;
            if (ticksSinceLastRead % 1000 == 0) {
                readButtons();
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
        controllerNumber = null;
        Integer controllerNum = getControllerNum();
        if (controllerNum != null) {
            controllerMapping = controllerMappings.get(GLFW.glfwGetJoystickGUID(controllerNum));
            if (controllerMapping != null) {
                System.out.println("Using controller " + controllerMapping.name);
            } else {
                System.out.println("No controller mapping found for " + GLFW.glfwGetJoystickGUID(controllerNum));
            }
        } else {
            controllerMapping = null;
        }
        Platform.runLater(this::calibrateTiming);
        registerListeners();
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
