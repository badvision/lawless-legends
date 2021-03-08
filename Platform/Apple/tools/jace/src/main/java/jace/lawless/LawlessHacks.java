package jace.lawless;

import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.lawless.LawlessVideo.RenderEngine;

/**
 * Hacks that affect lawless legends gameplay
 */
public class LawlessHacks extends Cheats {

    // Modes specified by the game engine
    int MODE_SOFTSWITCH_MIN = 0x0C049;
    int MODE_SOFTSWITCH_MAX = 0x0C04F;

    public LawlessHacks(Computer computer) {
        super(computer);
    }

    @Override
    public void toggleCheats() {
        // Do nothing -- you cannot toggle this once it's active.
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat(RAMEvent.TYPE.ANY, false, (e) -> {
            int addr = e.getAddress() ;
            if (addr >= MODE_SOFTSWITCH_MIN && e.getAddress()  <= MODE_SOFTSWITCH_MAX) {
                System.out.println("Trapped " + e.getType().toString() + " to $"+Integer.toHexString(e.getAddress()));
                setEngineByOrdinal(e.getAddress() - MODE_SOFTSWITCH_MIN);
            }
        }, MODE_SOFTSWITCH_MIN, MODE_SOFTSWITCH_MAX);
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
