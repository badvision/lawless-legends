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

package jace.apple2e;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.LawlessLegends;
import jace.apple2e.softswitch.VideoSoftSwitch;
import jace.cheat.Cheats;
import jace.config.ConfigurableField;
import jace.config.DeviceSelection;
import jace.core.Card;
import jace.core.Computer;
import jace.core.Device;
import jace.core.Motherboard;
import jace.core.RAM;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.Cards;
import jace.hardware.Joystick;
import jace.hardware.NoSlotClock;
import jace.hardware.VideoImpls;
import jace.hardware.ZipWarpAccelerator;
import jace.lawless.FPSMonitorDevice;
import jace.state.Stateful;

/**
 * Apple2e is a computer with a 65c02 CPU, 128k of bankswitched ram,
 * double-hires graphics, and up to seven peripheral I/O cards installed. Pause
 * and resume are implemented by the Motherboard class. This class provides
 * overall configuration of the computer, but the actual operation of the
 * computer and its timing characteristics are managed in the Motherboard class.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
@Stateful
public class Apple2e extends Computer {

    static int IRQ_VECTOR = 0x003F2;
    @ConfigurableField(name = "Slot 1", shortName = "s1card")
    public DeviceSelection<Cards> card1 = new DeviceSelection<>(Cards.class, null);
    @ConfigurableField(name = "Slot 2", shortName = "s2card")
    public DeviceSelection<Cards> card2 = new DeviceSelection<>(Cards.class, null);
    @ConfigurableField(name = "Slot 3", shortName = "s3card")
    public DeviceSelection<Cards> card3 = new DeviceSelection<>(Cards.class, null);
    @ConfigurableField(name = "Slot 4", shortName = "s4card")
    public DeviceSelection<Cards> card4 = new DeviceSelection<>(Cards.class, null);
    @ConfigurableField(name = "Slot 5", shortName = "s5card")
    public DeviceSelection<Cards> card5 = new DeviceSelection<>(Cards.class, null);
    @ConfigurableField(name = "Slot 6", shortName = "s6card")
    public DeviceSelection<Cards> card6 = new DeviceSelection<>(Cards.class, Cards.DiskIIDrive, true);
    @ConfigurableField(name = "Slot 7", shortName = "s7card")
    public DeviceSelection<Cards> card7 = new DeviceSelection<>(Cards.class, Cards.MassStorage, true);
    @ConfigurableField(name = "Debug rom", shortName = "debugRom", description = "Use debugger //e rom")
    public boolean useDebugRom = false;
    @ConfigurableField(name = "Helpful hints", shortName = "hints")
    public boolean enableHints = true;
    @ConfigurableField(name = "Renderer", shortName = "video", description = "Video rendering implementation")
    public DeviceSelection<VideoImpls> videoRenderer = new DeviceSelection<>(VideoImpls.class, VideoImpls.Lawless, false);
    @ConfigurableField(name = "Aux Ram", shortName = "ram", description = "Aux ram card")
    public DeviceSelection<RAM128k.RamCards> ramCard = new DeviceSelection<>(RAM128k.RamCards.class, RAM128k.RamCards.CardRamworks, false);
    @ConfigurableField(name = "Joystick 1 Enabled", shortName = "joy1", description = "If unchecked, then there is no joystick support.", enablesDevice = true)
    public boolean joy1enabled = true;
    @ConfigurableField(name = "Joystick 2 Enabled", shortName = "joy2", description = "If unchecked, then there is no joystick support.", enablesDevice = true)
    public boolean joy2enabled = false;
    @ConfigurableField(name = "No-Slot Clock Enabled", shortName = "clock", description = "If checked, no-slot clock will be enabled", enablesDevice = true)
    public boolean clockEnabled = true;
    @ConfigurableField(name = "Accelerator Enabled", shortName = "zip", description = "If checked, add support for Zip/Transwarp", enablesDevice = true)
    public boolean acceleratorEnabled = true;
    @ConfigurableField(name = "Production mode", shortName = "production")
    public boolean PRODUCTION_MODE = false;
    
    public Joystick joystick1;
    public Joystick joystick2;
    @ConfigurableField(name = "Activate Cheats", shortName = "cheat")
    public DeviceSelection<Cheats.Cheat> cheatEngine = new DeviceSelection<>(Cheats.Cheat.class, null);
    public Cheats activeCheatEngine = null;
    public NoSlotClock clock;
    public ZipWarpAccelerator accelerator;
    FPSMonitorDevice fpsCounters;
    @ConfigurableField(name = "Show speed monitors", shortName = "showFps")
    public boolean showSpeedMonitors = true;

    /**
     * Creates a new instance of Apple2e
     */
    public Apple2e() {
        super();
        fpsCounters = new FPSMonitorDevice();
        try {
            setCpu(new MOS65C02());
            setMotherboard(new Motherboard(null));
        } catch (Throwable t) {
            System.err.println("Unable to initialize virtual machine");
            t.printStackTrace(System.err);
        }
    }

    @Override
    public String getName() {
        return "Computer (Apple //e)";
    }

    
    @Override
    public void coldStart() {
        getMotherboard().whileSuspended(()->{
            System.err.println("Cold starting computer: RESETTING SOFT SWITCHES");
            for (SoftSwitches s : SoftSwitches.values()) {
                s.getSwitch().reset();
            }
            reconfigure();
            getMemory().configureActiveMemory();
            getVideo().configureVideoMode();
            for (Optional<Card> c : getMemory().getAllCards()) {
                c.ifPresent(Card::reset);
            }
        });
        reboot();
    }

    public void reboot() {
        RAM r = getMemory();
        r.write(IRQ_VECTOR, (byte) 0x00, false, true);
        r.write(IRQ_VECTOR + 1, (byte) 0x00, false, true);
        r.write(IRQ_VECTOR + 2, (byte) 0x00, false, true);
        warmStart();
    }

    @Override
    public void warmStart() {
        getMotherboard().whileSuspended(()->{
            // This isn't really authentic behavior but sometimes games like memory to have a consistent state when booting.
            for (SoftSwitches s : SoftSwitches.values()) {
                if (! (s.getSwitch() instanceof VideoSoftSwitch)) {
                    s.getSwitch().reset();
                }
            }
            ((RAM128k)getMemory()).zeroAllRam();
            getMemory().configureActiveMemory();
            getVideo().configureVideoMode();
            getCpu().reset();
            for (Optional<Card> c : getMemory().getAllCards()) {
                c.ifPresent(Card::reset);
            }
        });
        resume();
    }

    public Cheats getActiveCheatEngine() {
        return activeCheatEngine;
    }

    private void insertCard(DeviceSelection<Cards> type, int slot) {
        if (getMemory().getCard(slot).isPresent()) {
            if (type.getValue() != null && type.getValue().isInstance(getMemory().getCard(slot).get())) {
                return;
            }            
            getMemory().removeCard(slot);
        }
        if (type != null && type.getValue() != null) {
            Card card = type.getValue().create();
            getMemory().addCard(card, slot);
        }
    }

    private RAM128k.RamCards getDesiredMemoryConfiguration() {
        if (ramCard.getValue() == null) {
            return RAM128k.RamCards.CardExt80Col;
        } else {
            return ramCard.getValue();
        }
    }
    
    private boolean isMemoryConfigurationCorrect() {
        if (getMemory() == null) {
            return false;
        }
        return getDesiredMemoryConfiguration().isInstance((RAM128k) getMemory());
    }
    
    @Override
    protected RAM createMemory() {
        return getDesiredMemoryConfiguration().create();
    }

    @Override
    public void loadRom(boolean reload) throws IOException {
        if (!romLoaded.isDone() && reload) {
            if (useDebugRom) {
                loadRom("/jace/data/apple2e_debug.rom");
            } else {
                loadRom("/jace/data/apple2e.rom");
            }
        }
    }

    @Override
    public final void reconfigure() {
        super.reconfigure();

        if (Utility.isHeadlessMode()) {
            joy1enabled = false;
            joy2enabled = false;
        }

        if (getMotherboard() == null) {
            System.err.println("No motherboard, cannot reconfigure");
            Thread.dumpStack();
            return;
        }
        getMotherboard().whileSuspended(()-> {
            System.err.println("Reconfiguring computer...");
            if (!isMemoryConfigurationCorrect()) {
                if (getVideo() != null) {
                    getVideo().suspend();
                }
                setVideo(null);
                System.out.println("Creating new ram using " + getDesiredMemoryConfiguration().getName());
                setMemory(createMemory());
            }

            // Make sure all softswitches are configured after confirming memory exists
            for (SoftSwitches s : SoftSwitches.values()) {
                s.getSwitch().register();
            }

            try {
                loadRom(true);
            } catch (IOException e) {
                Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, "Failed to load system rom ROMs", e);
            }
            
            getMemory().configureActiveMemory();

            Set<Device> newDeviceSet = new HashSet<>();

            if (acceleratorEnabled) {
                if (accelerator == null) {
                    accelerator = new ZipWarpAccelerator();
                }
                newDeviceSet.add(accelerator);
            }

            if (joy1enabled) {
                if (joystick1 == null) {
                    joystick1 = new Joystick(0, this);
                }
                newDeviceSet.add(joystick1);
            } else {
                joystick1 = null;
            }

            if (joy2enabled) {
                if (joystick2 == null) {
                    joystick2 = new Joystick(1, this);
                }
                newDeviceSet.add(joystick2);
            } else {
                joystick2 = null;
            }

            if (clockEnabled) {
                if (clock == null) {
                    clock = new NoSlotClock();
                }
                newDeviceSet.add(clock);
            } else {
                clock = null;
            }

            if (videoRenderer.getValue() != null && !videoRenderer.getValue().isInstance(getVideo())) {
                boolean resumeVideo = false;
                if (getVideo() != null) {
                    resumeVideo = getVideo().suspend();
                }
                setVideo(videoRenderer.getValue().create());
                getVideo().configureVideoMode();
                getVideo().reconfigure();
                if (LawlessLegends.getApplication() != null) {
                    LawlessLegends.getApplication().reconnectUIHooks();
                }
                if (resumeVideo) {
                    getVideo().resume();
                }
            }

            // Add all new cards
            insertCard(card1, 1);
            insertCard(card2, 2);
            insertCard(card3, 3);
            insertCard(card4, 4);
            insertCard(card5, 5);
            insertCard(card6, 6);
            insertCard(card7, 7);
            if (enableHints) {
                enableHints();
            } else {
                disableHints();
            }

            if (cheatEngine.getValue() == null) {
                if (activeCheatEngine != null) {
                    activeCheatEngine.detach();
                    activeCheatEngine.suspend();
                    activeCheatEngine = null;
                }
            } else {
                boolean startCheats = true;
                if (activeCheatEngine != null) {
                    if (cheatEngine.getValue().isInstance(activeCheatEngine)) {
                        startCheats = false;
                        newDeviceSet.add(activeCheatEngine);
                    } else {
                        activeCheatEngine.detach();
                        activeCheatEngine.suspend();
                        activeCheatEngine = null;
                    }
                }
                if (startCheats && cheatEngine.getValue() != null) {
                    activeCheatEngine = cheatEngine.getValue().create();
                    newDeviceSet.add(activeCheatEngine);
                }
            }

            newDeviceSet.add(getCpu());
            newDeviceSet.add(getVideo());
            for (Optional<Card> c : getMemory().getAllCards()) {
                c.ifPresent(newDeviceSet::add);
            }                
            if (showSpeedMonitors) {
                newDeviceSet.add(fpsCounters);
            }
            getMotherboard().attach();
            getMotherboard().setAllDevices(newDeviceSet);
            getMotherboard().reconfigure();
        });
    }

    @Override
    protected void doPause() {
        if (getMotherboard() == null) {
            return;
        }
        getMotherboard().setPaused(true);
    }

    @Override
    protected void doResume() {
        if (getMotherboard() == null) {
            return;
        }
        getMotherboard().resume();
    }

//    public boolean isRunning() {
//        if (motherboard == null) {
//            return false;
//        }
//        return motherboard.isRunning() && !motherboard.isPaused;
//    }
    private final List<RAMListener> hints = new ArrayList<>();

    ScheduledExecutorService animationTimer = new ScheduledThreadPoolExecutor(1);
    Runnable drawHints = () -> {
        if (getCpu().getProgramCounter() >> 8 != 0x0c6) {
            return;
        }
        int row = 2;
        for (String s : new String[]{
            "              Welcome to",
            "         _    __    ___   ____ ",
            "        | |  / /\\  / / ` | |_  ",
            "      \\_|_| /_/--\\ \\_\\_, |_|__ ",
            "",
            "    Java Apple Computer Emulator",
            "",
            "        Presented by BLuRry",
            "       https://goo.gl/SnzqG",
            "",
            "To insert a disk, please drag it over",
            "this window and drop on the desired",
            "drive icon.",
            "",
            "Press CTRL+SHIFT+C for configuration.",
            "Press CTRL+SHIFT+I for IDE window.",
            "",
            "O-A: Alt/Option",
            "C-A: Shortcut/Command",
            "Reset: Delete/Backspace"
        }) {
            int addr = 0x0401 + VideoDHGR.calculateTextOffset(row++);
            for (char c : s.toCharArray()) {
                getMemory().write(addr++, (byte) (c | 0x080), false, true);
            }
        }
    };
    int animAddr, animCycleNumber;
    byte animOldValue;
    final String animation = "+xX*+-";
    ScheduledFuture<?> animationSchedule;
    Runnable doAnimation = () -> {
        if (animAddr == 0 || animCycleNumber >= animation.length()) {
            if (animAddr > 0) {
                getMemory().write(animAddr, animOldValue, true, true);
            }
            int animX = (int) (Math.random() * 24.0) + 7;
            int animY = (int) (Math.random() * 3.0) + 3;
            animAddr = 0x0400 + VideoDHGR.calculateTextOffset(animY) + animX;
            animOldValue = getMemory().readRaw(animAddr);
            animCycleNumber = 0;
        }
        if (getCpu().getProgramCounter() >> 8 == 0x0c6) {
            getMemory().write(animAddr, (byte) (animation.charAt(animCycleNumber) | 0x080), true, true);
            animCycleNumber++;
        } else {
            getMemory().write(animAddr, animOldValue, true, true);
            animationSchedule.cancel(false);
            animAddr = 0;
        }
    };

    private void enableHints() {
        if (hints.isEmpty()) {
            hints.add(getMemory().observe("Helpful hints", RAMEvent.TYPE.EXECUTE, 0x0FB63, (e)->{
                animationTimer.schedule(drawHints, 1, TimeUnit.SECONDS);
                animationSchedule =
                            animationTimer.scheduleAtFixedRate(doAnimation, 1250, 100, TimeUnit.MILLISECONDS);
            }));
            // Latch to the PRODOS SYNTAX CHECK parser
            /*
             hints.add(new RAMListener(RAMEvent.TYPE.EXECUTE, RAMEvent.SCOPE.ADDRESS, RAMEvent.VALUE.ANY) {
             @Override
             protected void doConfig() {setScopeStart(0x0a685);}

             @Override
             protected void doEvent(RAMEvent e) {
             String in = "";
             for (int i=0x0200; i < 0x0300; i++) {
             char c = (char) (getMemory().readRaw(i) & 0x07f);
             if (c == 0x0d) break;
             in += c;
             }

             System.err.println("Intercepted command: "+in);
             }
             });
             */
        }
    }

    private void disableHints() {
        hints.forEach((hint) -> getMemory().removeListener(hint));
        hints.clear();
    }

    @Override
    public String getShortName() {
        return "computer";
    }
}