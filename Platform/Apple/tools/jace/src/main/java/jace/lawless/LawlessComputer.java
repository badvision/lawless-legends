package jace.lawless;

import jace.apple2e.Apple2e;
import jace.apple2e.RAM128k;
import jace.apple2e.SoftSwitches;
import jace.apple2e.VideoNTSC;
import jace.config.ConfigurableField;
import jace.core.Card;
import jace.core.Video;
import jace.library.MediaConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        motherboard.whileSuspended(this::initLawlessLegendsConfiguration);
    }

    private void initLawlessLegendsConfiguration() {
        reconfigure();  // Required before anything so that memory is initialized
        this.cheatEngine.setValue(LawlessHacks.class);
        this.activeCheatEngine = new LawlessHacks(this);
        this.activeCheatEngine.attach();
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
            finishColdStart();
            getMotherboard().requestSpeed(this);
        }
    }

    public void startAnimation() {
        getCpu().suspend();
        SoftSwitches._80COL.getSwitch().setState(true);
        SoftSwitches.TEXT.getSwitch().setState(false);
        SoftSwitches.HIRES.getSwitch().setState(true);
        SoftSwitches.PAGE2.getSwitch().setState(false);
        SoftSwitches.DHIRES.getSwitch().setState(true);
        ((VideoNTSC) getVideo()).enableVideo7 = false;
        getMemory().configureActiveMemory();
        getVideo().configureVideoMode();
        doResume();

        if (!performedBootAnimation) {
            try {
                performedBootAnimation = true;
                waitForVBL();
                renderWithMask(0x8, 0x11, 0x22, 0x44);
                Video.forceRefresh();
                waitForVBL(10);
                renderWithMask(0x4c, 0x19, 0x33, 0x66);
                Video.forceRefresh();
                waitForVBL(10);
                renderWithMask(0x6e, 0x5d, 0x3B, 0x77);
                Video.forceRefresh();
                waitForVBL(10);
                renderWithMask(0x7f, 0x7f, 0x7f, 0x7f);
                Video.forceRefresh();
                waitForVBL(250);
//                renderWithMask(0x6e, 0x5d, 0x3B, 0x77);
                renderWithMask(0x77, 0x6e, 0x5d, 0x3b);
                Video.forceRefresh();
                waitForVBL(10);
//                renderWithMask(0x4c, 0x19, 0x33, 0x66);
                renderWithMask(0x33, 0x66, 0x4c, 0x19);
                Video.forceRefresh();
                waitForVBL(10);
//                renderWithMask(0x8, 0x11, 0x22, 0x44);
                renderWithMask(0x11, 0x22, 0x44, 0x8);
                Video.forceRefresh();
                waitForVBL(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(LawlessComputer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        finishColdStart();

    }

    private void renderWithMask(int i1, int i2, int i3, int i4) {
        RAM128k ram = (RAM128k) getMemory();
        byte[] framebuffer = getBootScreen();
        for (int i = 0; i < 0x02000; i += 2) {
            int next = 0;
            if (i < 0x02000) {
                next = (framebuffer[i] & 1) << 6;
            }
            Byte b1 = (byte) ((framebuffer[i + 0x02000] & 0x07f) >> 1 | next);
            ram.getAuxMemory().writeByte(0x02000 + i, (byte) (b1 & i1 | 0x080));
            if (i < 0x01FFF) {
                next = (framebuffer[i + 0x02001] & 1) << 6;
            }
            Byte b2 = (byte) ((framebuffer[i] & 0x07f) >> 1 | next);
            ram.getMainMemory().writeByte(0x02000 + i, (byte) (b2 & i2 | 0x080));
            if (i < 0x01FFF) {
                next = (framebuffer[i + 1] & 1) << 6;
            }
            Byte b3 = (byte) ((framebuffer[i + 0x02001] & 0x07f) >> 1 | next);
            ram.getAuxMemory().writeByte(0x02001 + i, (byte) (b3 & i3 | 0x080));
            if (i < 0x01FFE) {
                next = (framebuffer[i + 0x02002] & 1) << 6;
            }
            Byte b4 = (byte) ((framebuffer[i + 1] & 0x07f) >> 1 | next);
            ram.getMainMemory().writeByte(0x02001 + i, (byte) (b4 & i4 | 0x080));
        }
    }

    List<Runnable> vblCallbacks = Collections.synchronizedList(new ArrayList<>());

    public void waitForVBL() throws InterruptedException {
        waitForVBL(0);
    }

    public void waitForVBL(int count) throws InterruptedException {
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
            for (Optional<Card> c : getMemory().getAllCards()) {
                c.ifPresent(Card::reset);
            }
            waitForVBL();
            reboot();
        } catch (InterruptedException ex) {
            Logger.getLogger(LawlessComputer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private byte[] getBootScreen() {
        if (bootScreen == null) {
            InputStream in = getClass().getClassLoader().getResourceAsStream("jace/data/bootscreen.bin");
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
