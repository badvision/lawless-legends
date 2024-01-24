package jace.lawless;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.apple2e.Apple2e;
import jace.apple2e.RAM128k;
import jace.apple2e.SoftSwitches;
import jace.apple2e.VideoNTSC;
import jace.cheat.Cheats;
import jace.config.ConfigurableField;
import jace.config.Configuration;
import jace.core.Video;
import jace.library.MediaConsumer;

/**
 * Extends standard implementation to provide different cold start behavior
 */
public class LawlessComputer extends Apple2e {

    byte[] bootScreen = null;
    boolean performedBootAnimation = false;
    LawlessImageTool gameDiskHandler = new LawlessImageTool();
    @ConfigurableField(name = "Boot Animation")
    public boolean showBootAnimation = true;

    public LawlessComputer() {
        super();
    }
    
    public void initLawlessLegendsConfiguration() {
        this.cheatEngine.setValue(Cheats.Cheat.LawlessHacks);
        reconfigure();  // Required before anything so that memory is initialized
        // this.activeCheatEngine = new LawlessHacks(this);
        // this.activeCheatEngine.attach();
        blankTextPage1();
        reconfigure();        
    }
    
    private void blankTextPage1() {
        // Fill text page 1 with spaces
        for (int i = 0x0400; i < 0x07FF; i++) {
            getMemory().write(i, (byte) (0x080 | ' '), false, false);
        }
    }

    @Override
    public void coldStart() {
        motherboard.whileSuspended(()->{
            RAM128k ram = (RAM128k) getMemory();
            ram.zeroAllRam();
            blankTextPage1();
            for (SoftSwitches s : SoftSwitches.values()) {
                s.getSwitch().reset();
            }
        });
        if (showBootAnimation && PRODUCTION_MODE) {
            (new Thread(this::startAnimation)).start();
        } else {
            cpu.setPaused(false);
            finishColdStart();
        }
    }

    public void startAnimation() {
        cpu.setPaused(true);
        for (SoftSwitches s : SoftSwitches.values()) {
            s.getSwitch().reset();
        }
        SoftSwitches._80COL.getSwitch().setState(true);
        SoftSwitches.TEXT.getSwitch().setState(false);
        SoftSwitches.HIRES.getSwitch().setState(true);
        SoftSwitches.PAGE2.getSwitch().setState(false);
        SoftSwitches.DHIRES.getSwitch().setState(true);
        ((VideoNTSC) getVideo()).enableVideo7 = false;
        getMemory().configureActiveMemory();
        getVideo().configureVideoMode();
        Configuration.registerKeyHandlers();
        doResume();

        if (!performedBootAnimation) {
            try {
                performedBootAnimation = true;
                waitForVBL();
                renderWithMask(0x00,0x00,0x00,0x00);
                renderWithMask(0x08,0x10,0x20,0x40,0x00,0x01,0x02,0x04);
                renderWithMask(0x08,0x11,0x22,0x44);
                renderWithMask(0x0C,0x19,0x32,0x64,0x48,0x11,0x23,0x46);
                renderWithMask(0x4C,0x19,0x33,0x66);
                renderWithMask(0x4E,0x1D,0x3B,0x76,0x6C,0x59,0x33,0x67);
                renderWithMask(0x6E,0x5D,0x3B,0x77);
                renderWithMask(0x7F,0x7E,0x7D,0x7B,0x77,0x6F,0x5F,0x3F);
                renderWithMask(0x7F,0x7F,0x7F,0x7F);
                waitForVBL(230);
                renderWithMask(0x77,0x6F,0x5F,0x3F,0x7F,0x7E,0x7D,0x7B);
                renderWithMask(0x77,0x6E,0x5D,0x3B);
                renderWithMask(0x73,0x66,0x4D,0x1B,0x37,0x6E,0x5C,0x39);
                renderWithMask(0x33,0x66,0x4C,0x19);
                renderWithMask(0x31,0x62,0x44,0x09,0x13,0x26,0x4C,0x18);
                renderWithMask(0x11,0x22,0x44,0x08);
                renderWithMask(0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x00);
                renderWithMask(0x00,0x00,0x00,0x00);
            } catch (InterruptedException ex) {
                Logger.getLogger(LawlessComputer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        cpu.setPaused(false);
        finishColdStart();

    }

    private void renderWithMask(int... mask) throws InterruptedException {
        RAM128k ram = (RAM128k) getMemory();
        byte[] framebuffer = getBootScreen();
        int maskOffset;
        for (int i = 0; i < 0x02000; i += 2) {
            int y = Video.identifyHiresRow(i + 0x02000);
            int x = i - Video.calculateHiresOffset(y);
            if (y < 0 || y >= 192 || x >= 40 || x < 0) {
                continue;
            }
            maskOffset = (y % 2) * 4;
            maskOffset += ((x / 2) % 2) * 4;
            maskOffset %= mask.length;

            int next = (framebuffer[i] & 1) << 6;
            Byte b1 = (byte) ((framebuffer[i + 0x02000] & 0x07f) >> 1 | next);
            ram.getAuxMemory().writeByte(0x02000 + i, (byte) (b1 & mask[maskOffset] | 0x080));

            if (i < 0x01FFF) {
                next = (framebuffer[i + 0x02001] & 1) << 6;
                Byte b2 = (byte) ((framebuffer[i] & 0x07f) >> 1 | next);
                ram.getMainMemory().writeByte(0x02000 + i, (byte) (b2 & mask[maskOffset + 1] | 0x080));

                next = (framebuffer[i + 1] & 1) << 6;
                Byte b3 = (byte) ((framebuffer[i + 0x02001] & 0x07f) >> 1 | next);
                ram.getAuxMemory().writeByte(0x02001 + i, (byte) (b3 & mask[maskOffset + 2] | 0x080));
            }

            if (i < 0x01FFE) {
                next = (framebuffer[i + 0x02002] & 1) << 6;
                Byte b4 = (byte) ((framebuffer[i + 1] & 0x07f) >> 1 | next);
                ram.getMainMemory().writeByte(0x02001 + i, (byte) (b4 & mask[maskOffset + 3] | 0x080));
            }
        }
        Video.forceRefresh();
        waitForVBL(5);
    }

    List<Runnable> vblCallbacks = Collections.synchronizedList(new ArrayList<>());

    public void waitForVBL() throws InterruptedException {
        waitForVBL(0);
    }

    public void waitForVBL(int count) throws InterruptedException {
        if (getVideo() == null || !getVideo().isRunning()) {
            return;
        }
        Semaphore s = new Semaphore(0);
        onNextVBL(s::release);
        s.acquire();
        if (count > 1) {
            waitForVBL(count - 1);
        }
    }

    public void onNextVBL(Runnable r) {
        vblCallbacks.add(r);
    }

    @Override
    public void notifyVBLStateChanged(boolean state) {
        super.notifyVBLStateChanged(state);
        if (state) {
            while (vblCallbacks != null && !vblCallbacks.isEmpty()) {
                vblCallbacks.remove(0).run();
            }
        }
    }

    public void finishColdStart() {
        try {
            waitForVBL();
            reboot();
        } catch (InterruptedException ex) {
            Logger.getLogger(LawlessComputer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private byte[] getBootScreen() {
        if (bootScreen == null) {
            InputStream in = getClass().getResourceAsStream("/jace/data/bootscreen.bin");
            bootScreen = new byte[0x04000];
            int len, offset = 0;
            try {
                while (offset < 0x04000 && (len = in.read(bootScreen, offset, 0x04000 - offset)) > 0) {
                    offset += len;
                }
            } catch (IOException ex) {
                Logger.getLogger(LawlessComputer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return bootScreen;
    }

    public MediaConsumer getUpgradeHandler() {
        return gameDiskHandler;
    }
}
