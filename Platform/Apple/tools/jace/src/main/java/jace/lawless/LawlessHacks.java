package jace.lawless;

import jace.apple2e.MOS65C02;
import jace.apple2e.SoftSwitches;
import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.lawless.LawlessVideo.RenderEngine;

/**
 * Hacks that affect lawless legends gameplay
 */
public class LawlessHacks extends Cheats {

    // Location of font routines
    int FONT_ROUTINES = 0x0EC00;
    int FONT_ROUTINES_END = 0x0f800;
    int FONT_SPEEDUP_CYCLES = 10000;
    int FONT_ROUTINES_LEN = 0x0f00;
    // Modes specified by the game engine
    int MODE_SOFTSWITCH = 0x0C020;
    
    
    public LawlessHacks(Computer computer) {
        super(computer);
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat(RAMEvent.TYPE.WRITE, (e) -> {
            if (e.getAddress() >= 0x02000 && e.getAddress() <= 0x05FFF) {
                ((LawlessVideo) computer.getVideo()).setBWFlag(e.getAddress(),
                        !SoftSwitches.RAMWRT.getState() ||
                        computer.getCpu().getProgramCounter() < FONT_ROUTINES ||
                        computer.getCpu().getProgramCounter() > FONT_ROUTINES_END);
            }
        }, 0x02000, 0x05FFF);
        // Watch for font routine usage for speedup
        addCheat(RAMEvent.TYPE.EXECUTE, (e) -> {
            if ((e.getAddress() & 0x0ff00) == FONT_ROUTINES) {
                computer.motherboard.requestSpeed(this);
            }
        }, FONT_ROUTINES, FONT_ROUTINES | 0x0ff);
        // Try to detect engines changing
        addCheat(RAMEvent.TYPE.ANY, false, (e) -> {
            if ((e.getAddress() & 0x0FFF0) == MODE_SOFTSWITCH) {
                System.out.println("Trapped " + e.getType().toString() + " to $"+Integer.toHexString(e.getAddress()));
                setEngineByOrdinal(e.getAddress() - MODE_SOFTSWITCH);
            }
        }, MODE_SOFTSWITCH, MODE_SOFTSWITCH | 0x0f);
    }

    @Override
    public String getDeviceName() {
        return "Lawless Legends optimizations";
    }

    @Override
    public void tick() {
    }

    private void setEngineByOrdinal(int mode) {
        LawlessVideo video = (LawlessVideo) computer.getVideo();
        if (mode >= 0 && mode < RenderEngine.values().length) {
            video.setEngine(RenderEngine.values()[mode]);
        } else {
            video.setEngine(RenderEngine.UNKNOWN);
        }
    }
}