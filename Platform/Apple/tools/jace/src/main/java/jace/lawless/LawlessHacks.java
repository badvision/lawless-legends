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
    int MODE_SOFTSWITCH = 0x0C020;


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