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
    int ENGINE_ADDR = 0x06000;
    int ENGINE_FIRST_OPCODE = ENGINE_ADDR + (13 * 3);
    int DETECT_ENGINE_WRITE = 0x060FF;

    public LawlessHacks(Computer computer) {
        super(computer);
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat(RAMEvent.TYPE.WRITE, (e) -> {
            if (e.getAddress() >= 0x02000 && e.getAddress() <= 0x05FFF) {
                ((LawlessVideo) computer.getVideo()).setBWFlag(e.getAddress(),
                        SoftSwitches.RAMWRT.getState() ||
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
        addCheat(RAMEvent.TYPE.WRITE, false, (e) -> {
            if (e.getAddress() == DETECT_ENGINE_WRITE) {
                detectActiveEngine();
            }
        }, DETECT_ENGINE_WRITE);
    }

    @Override
    public String getDeviceName() {
        return "Lawless Legends optimizations";
    }

    @Override
    public void tick() {
    }

    private void detectActiveEngine() {
        LawlessVideo video = (LawlessVideo) computer.getVideo();
//        for (int i = 0x06000; i < 0x06080;) {
//            System.out.printf("%04x: ", i);
//            for (int j = 0; j < 16; j++, i++) {
//                System.out.printf("%02x ", computer.getMemory().readRaw(i) & 0x0ff);
//            }
//            System.out.println();
//        }
        int firstPageByte = computer.getMemory().readRaw(ENGINE_ADDR) & 0x0ff;
        int firstDataByte = computer.getMemory().readRaw(ENGINE_FIRST_OPCODE) & 0x0ff;
        int secondDataByte = computer.getMemory().readRaw(ENGINE_FIRST_OPCODE + 1) & 0x0ff;
        if (firstPageByte == MOS65C02.OPCODE.JMP_AB.getCode()
                && firstDataByte == MOS65C02.OPCODE.LDX_ZP.getCode()) {
            // 2D Engine: First instruction is LDX MAP_PARTITION
            video.setEngine(RenderEngine._2D);
        } else if (firstPageByte == MOS65C02.OPCODE.JMP_AB.getCode()
                && firstDataByte == 0
                && secondDataByte == 0) {
            // 3D Engine: First byte is a zero for MapHeader
            video.setEngine(RenderEngine._3D);
        } else if (firstPageByte == MOS65C02.OPCODE.JMP_AB.getCode()
                && firstDataByte == 0
                && secondDataByte == 0x067) {
            // 3D Engine: First byte is a zero for MapHeader
            video.setEngine(RenderEngine.PORTRAIT);
        } else {
            video.setEngine(RenderEngine.UNKNOWN);
        }
    }

}
