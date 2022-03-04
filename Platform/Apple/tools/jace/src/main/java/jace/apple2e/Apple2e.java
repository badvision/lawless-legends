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
package jace.apple2e;

import jace.LawlessLegends;
import jace.apple2e.softswitch.VideoSoftSwitch;
import jace.cheat.Cheats;
import jace.config.ClassSelection;
import jace.config.ConfigurableField;
import jace.core.*;
import jace.hardware.*;
import jace.hardware.massStorage.CardMassStorage;
import jace.lawless.FPSMonitorDevice;
import jace.lawless.LawlessVideo;
import jace.state.Stateful;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public ClassSelection<Card> card1 = new ClassSelection<>(Card.class, null);
    @ConfigurableField(name = "Slot 2", shortName = "s2card")
    public ClassSelection<Card> card2 = new ClassSelection<>(Card.class, null);
    @ConfigurableField(name = "Slot 3", shortName = "s3card")
    public ClassSelection<Card> card3 = new ClassSelection<>(Card.class, null);
    @ConfigurableField(name = "Slot 4", shortName = "s4card")
    public ClassSelection<Card> card4 = new ClassSelection<>(Card.class, null);
    @ConfigurableField(name = "Slot 5", shortName = "s5card")
    public ClassSelection<Card> card5 = new ClassSelection<>(Card.class, null);
    @ConfigurableField(name = "Slot 6", shortName = "s6card")
    public ClassSelection<Card> card6 = new ClassSelection<>(Card.class, CardDiskII.class);
    @ConfigurableField(name = "Slot 7", shortName = "s7card")
    public ClassSelection<Card> card7 = new ClassSelection<>(Card.class, CardMassStorage.class);
    @ConfigurableField(name = "Debug rom", shortName = "debugRom", description = "Use debugger //e rom")
    public boolean useDebugRom = false;
    @ConfigurableField(name = "Console probe", description = "Enable console redirection (experimental!)")
    public boolean useConsoleProbe = false;
    private final ConsoleProbe probe = new ConsoleProbe();
    @ConfigurableField(name = "Helpful hints", shortName = "hints")
    public boolean enableHints = true;
    @ConfigurableField(name = "Renderer", shortName = "video", description = "Video rendering implementation")
    public ClassSelection<Video> videoRenderer = new ClassSelection<>(Video.class, LawlessVideo.class);
    @ConfigurableField(name = "Aux Ram", shortName = "ram", description = "Aux ram card")
    public ClassSelection<RAM128k> ramCard = new ClassSelection<>(RAM128k.class, CardRamworks.class);
    @ConfigurableField(name = "Joystick 1 Enabled", shortName = "joy1", description = "If unchecked, then there is no joystick support.", enablesDevice = true)
    public boolean joy1enabled = true;
    @ConfigurableField(name = "Joystick 2 Enabled", shortName = "joy2", description = "If unchecked, then there is no joystick support.", enablesDevice = true)
    public boolean joy2enabled = false;
    @ConfigurableField(name = "No-Slot Clock Enabled", shortName = "clock", description = "If checked, no-slot clock will be enabled", enablesDevice = true)
    public boolean clockEnabled = true;
    @ConfigurableField(name = "Accelerator Enabled", shortName = "zip", description = "If checked, add support for Zip/Transwarp", enablesDevice = true)
    public boolean acceleratorEnabled = true;
    @ConfigurableField(name = "Production mode", shortName = "production")
    public boolean PRODUCTION_MODE = true;
    
    public Joystick joystick1;
    public Joystick joystick2;
    @ConfigurableField(name = "Activate Cheats", shortName = "cheat")
    public ClassSelection<Cheats> cheatEngine = new ClassSelection<>(Cheats.class, null);
    public Cheats activeCheatEngine = null;
    public NoSlotClock clock;
    public ZipWarpAccelerator accelerator;
    FPSMonitorDevice fpsCounters;
    @ConfigurableField(name = "Show speed monitors", shortName = "showFps")
    public boolean showSpeedMonitors = false;

    /**
     * Creates a new instance of Apple2e
     */
    public Apple2e() {
        super();
        fpsCounters = new FPSMonitorDevice(this);
        try {
            setCpu(new MOS65C02(this));
            setMotherboard(new Motherboard(this, null));
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
        motherboard.whileSuspended(()->{
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
        motherboard.whileSuspended(()->{
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

    private void insertCard(Class<? extends Card> type, int slot) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        if (getMemory().getCard(slot).isPresent()) {
            if (getMemory().getCard(slot).get().getClass().equals(type)) {
                return;
            }
            getMemory().removeCard(slot);
        }
        if (type != null) {
            try {
                Card card = type.getConstructor(Computer.class).newInstance(this);
                getMemory().addCard(card, slot);
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Class<? extends RAM128k> getDesiredMemoryConfiguration() {
        if (ramCard.getValue() == null) {
            return CardExt80Col.class;
        } else {
            return ramCard.getValue();
        }
    }
    
    private boolean isMemoryConfigurationCorrect() {
        if (getMemory() == null) {
            return false;
        }
        return getMemory().getClass().equals(getDesiredMemoryConfiguration());
    }
    
    @Override
    public final void reconfigure() {
        super.reconfigure();

        if (Utility.isHeadlessMode()) {
            joy1enabled = false;
            joy2enabled = false;
        }

        if (motherboard == null) {
            return;
        }
        motherboard.whileSuspended(()-> {
            if (getMemory() != null) {
                for (SoftSwitches s : SoftSwitches.values()) {
                    s.getSwitch().unregister();
                }
            }
            if (!isMemoryConfigurationCorrect()) {
                try {
                    System.out.println("Creating new ram using " + getDesiredMemoryConfiguration().getName());
                    RAM128k newMemory = getDesiredMemoryConfiguration().getConstructor(Computer.class).newInstance(this);

                    if (getMemory() != null) {
                        newMemory.copyFrom((RAM128k) getMemory());
                    }
                    setMemory(newMemory);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
                }                
            }
            for (SoftSwitches s : SoftSwitches.values()) {
                s.getSwitch().register(this);
            }

            try {
                if (useDebugRom) {
                    loadRom("jace/data/apple2e_debug.rom");
                } else {
                    loadRom("jace/data/apple2e.rom");
                }
            } catch (IOException ex) {
                Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
            }

            getMemory().configureActiveMemory();

            Set<Device> newDeviceSet = new HashSet<>();

            if (acceleratorEnabled) {
                if (accelerator == null) {
                    accelerator = new ZipWarpAccelerator(this);
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
                    clock = new NoSlotClock(this);
                }
                newDeviceSet.add(clock);
            } else {
                clock = null;
            }

            if (useConsoleProbe) {
                probe.init(this);
            } else {
                probe.shutdown();
            }

            if (getVideo() == null || getVideo().getClass() != videoRenderer.getValue()) {
                boolean resumeVideo = false;
                if (getVideo() != null) {
                    resumeVideo = getVideo().suspend();
                }
                try {
                    setVideo(videoRenderer.getValue().getConstructor(Computer.class).newInstance(this));
                    getVideo().configureVideoMode();
                    getVideo().reconfigure();
                    if (LawlessLegends.getApplication() != null) {
                        LawlessLegends.getApplication().reconnectUIHooks();
                    }
                    if (resumeVideo) {
                        getVideo().resume();
                    }
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try {
                // Add all new cards
                insertCard(card1.getValue(), 1);
                insertCard(card2.getValue(), 2);
                insertCard(card3.getValue(), 3);
                insertCard(card4.getValue(), 4);
                insertCard(card5.getValue(), 5);
                insertCard(card6.getValue(), 6);
                insertCard(card7.getValue(), 7);
            } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (enableHints) {
                enableHints();
            } else {
                disableHints();
            }

            if (cheatEngine.getValue() == null) {
                if (activeCheatEngine != null) {
                    activeCheatEngine.detach();
                    newDeviceSet.add(activeCheatEngine);
                }
                activeCheatEngine = null;
            } else {
                boolean startCheats = true;
                if (activeCheatEngine != null) {
                    if (activeCheatEngine.getClass().equals(cheatEngine.getValue())) {
                        startCheats = false;
                    } else {
                        activeCheatEngine = null;
                    }
                }
                if (startCheats) {
                    try {
                        activeCheatEngine = cheatEngine.getValue().getConstructor(Computer.class).newInstance(this);
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(Apple2e.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    newDeviceSet.add(activeCheatEngine);
                }
            }

            newDeviceSet.add(cpu);
            newDeviceSet.add(video);
            for (Optional<Card> c : getMemory().getAllCards()) {
                c.ifPresent(newDeviceSet::add);
            }                
            if (showSpeedMonitors) {
                newDeviceSet.add(fpsCounters);
            }
            motherboard.attach();
            motherboard.setAllDevices(newDeviceSet);
            motherboard.reconfigure();
        });
    }

    @Override
    protected void doPause() {
        if (motherboard == null) {
            return;
        }
        motherboard.pause();
    }

    @Override
    protected void doResume() {
        if (motherboard == null) {
            return;
        }
        motherboard.resume();
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
    ScheduledFuture animationSchedule;
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
            hints.add(getMemory().observe(RAMEvent.TYPE.EXECUTE, 0x0FB63, (e)->{
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
    }

    @Override
    public String getShortName() {
        return "computer";
    }
}