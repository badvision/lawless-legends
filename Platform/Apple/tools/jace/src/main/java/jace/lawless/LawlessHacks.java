package jace.lawless;

import jace.apple2e.MOS65C02;
import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.core.Utility;
import jace.lawless.LawlessVideo.RenderEngine;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hacks that affect lawless legends gameplay
 */
public class LawlessHacks extends Cheats {

    Computer computer;
    // Location of font routines
    int FONT_ROUTINES = 0x0EC00;
    int FONT_SPEEDUP_CYCLES = 10000;
    int FONT_ROUTINES_LEN = 0x0f00;
    int ENGINE_ADDR = 0x06000;
    int ENGINE_FIRST_OPCODE = ENGINE_ADDR + (13 * 3);
    int DETECT_ENGINE_WRITE = 0x060FF;
    Cheats speedupRequester = this;
    AtomicInteger speedupCounter = new AtomicInteger();

    public LawlessHacks(Computer computer) {
        super(computer);
        this.computer = computer;
    }

    @Override
    public void registerListeners() {
        for (int entry = 0; entry < 13; entry++) {
            int targetAddress = FONT_ROUTINES + (entry * 3);
            addCheat(RAMEvent.TYPE.EXECUTE, (e) -> {
                if (e.getAddress() == targetAddress) {
                    computer.motherboard.requestSpeed(speedupRequester);
                    speedupCounter.set(FONT_SPEEDUP_CYCLES);
                }
            }, targetAddress);
        }
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
        if (speedupCounter.get() > 0 && speedupCounter.decrementAndGet() <= 0) {
            int pc = computer.getCpu().getProgramCounter();
            if (pc >= FONT_ROUTINES && pc <= FONT_ROUTINES + FONT_ROUTINES_LEN) {
                speedupCounter.addAndGet(500);
                computer.motherboard.requestSpeed(speedupRequester);
            } else {
                computer.motherboard.cancelSpeedRequest(speedupRequester);
            }
        }
    }

    private void detectActiveEngine() {
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
            LawlessVideo.setEngine(RenderEngine._2D);
        } else if (firstPageByte == MOS65C02.OPCODE.JMP_AB.getCode()
                && firstDataByte == 0
                && secondDataByte == 0) {
            // 3D Engine: First byte is a zero for MapHeader
            LawlessVideo.setEngine(RenderEngine._3D);
        } else if (firstPageByte == MOS65C02.OPCODE.JMP_AB.getCode()
                && firstDataByte == 0
                && secondDataByte == 0x067) {
            // 3D Engine: First byte is a zero for MapHeader
            LawlessVideo.setEngine(RenderEngine.PORTRAIT);
        } else {
            LawlessVideo.setEngine(RenderEngine.UNKNOWN);
        }
    }

}
