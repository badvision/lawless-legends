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

import jace.core.CPU;
import jace.core.Card;
import jace.core.Computer;
import jace.core.PagedMemory;
import jace.core.RAM;
import jace.state.Stateful;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a 128k memory space and the MMU found in an Apple //e. The
 * MMU behavior is mimicked by configureActiveMemory.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
@Stateful
abstract public class RAM128k extends RAM {

    Logger LOG = Logger.getLogger(RAM128k.class.getName());

    Map<String, PagedMemory> banks;
    String state = "???";

    private Map<String, PagedMemory> getBanks() {
        if (banks == null) {
            banks = new HashMap<>();

            banks.put("main", mainMemory);
            banks.put("lc", languageCard);
            banks.put("lc2", languageCard2);
            banks.put("//e rom (80-col)", cPageRom);
            banks.put("//e rom", rom);
            banks.put("blank", blank);
            banks.put("aux", getAuxMemory());
            banks.put("aux lc", getAuxLanguageCard());
            banks.put("aux lc2", getAuxLanguageCard2());
            cards[1].ifPresent(c -> banks.put("card1a", c.getCxRom()));
            cards[1].ifPresent(c -> banks.put("card1b", c.getC8Rom()));
            cards[2].ifPresent(c -> banks.put("card2a", c.getCxRom()));
            cards[2].ifPresent(c -> banks.put("card2b", c.getC8Rom()));
            cards[3].ifPresent(c -> banks.put("card3a", c.getCxRom()));
            cards[3].ifPresent(c -> banks.put("card3b", c.getC8Rom()));
            cards[4].ifPresent(c -> banks.put("card4a", c.getCxRom()));
            cards[4].ifPresent(c -> banks.put("card4b", c.getC8Rom()));
            cards[5].ifPresent(c -> banks.put("card5a", c.getCxRom()));
            cards[5].ifPresent(c -> banks.put("card5b", c.getC8Rom()));
            cards[6].ifPresent(c -> banks.put("card6a", c.getCxRom()));
            cards[6].ifPresent(c -> banks.put("card6b", c.getC8Rom()));
            cards[7].ifPresent(c -> banks.put("card7a", c.getCxRom()));
            cards[7].ifPresent(c -> banks.put("card7b", c.getC8Rom()));
        }

        return banks;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void performExtendedCommand(int param) {
        switch (param) {
            case 0xda:
                // 64 da : Dump all memory mappings
                System.out.println("Active banks");
                for (int i = 0; i < 256; i++) {
                    byte[] read = activeRead.get(i);
                    byte[] write = activeWrite.get(i);
                    String readBank = getBanks().keySet().stream().filter(bank -> {
                        PagedMemory mem = getBanks().get(bank);
                        for (byte[] page : mem.getMemory()) {
                            if (page == read) {
                                return true;
                            }
                        }
                        return false;
                    }).findFirst().orElse("unknown");
                    String writeBank = getBanks().keySet().stream().filter(bank -> {
                        PagedMemory mem = getBanks().get(bank);
                        for (byte[] page : mem.getMemory()) {
                            if (page == write) {
                                return true;
                            }
                        }
                        return false;
                    }).findFirst().orElse("unknown");
                    LOG.log(Level.INFO, "Bank {0}\t{1}\t{2}", new Object[]{Integer.toHexString(i), readBank, writeBank});
                }
            default:
        }
    }

    @Stateful
    public PagedMemory mainMemory;
    @Stateful
    public PagedMemory languageCard;
    @Stateful
    public PagedMemory languageCard2;
    public PagedMemory cPageRom;
    public PagedMemory rom;
    public PagedMemory blank;

    public RAM128k(Computer computer) {
        super(computer);
        mainMemory = new PagedMemory(0xc000, PagedMemory.Type.RAM, computer);
        rom = new PagedMemory(0x3000, PagedMemory.Type.FIRMWARE_MAIN, computer);
        cPageRom = new PagedMemory(0x1000, PagedMemory.Type.SLOW_ROM, computer);
        languageCard = new PagedMemory(0x3000, PagedMemory.Type.LANGUAGE_CARD, computer);
        languageCard2 = new PagedMemory(0x1000, PagedMemory.Type.LANGUAGE_CARD, computer);
        activeRead = new PagedMemory(0x10000, PagedMemory.Type.RAM, computer);
        activeWrite = new PagedMemory(0x10000, PagedMemory.Type.RAM, computer);
        blank = new PagedMemory(0x100, PagedMemory.Type.RAM, computer);
        zeroAllRam();
    }

    public final void initMemoryPattern(PagedMemory mem) {
        // Format memory with FF FF 00 00 pattern
        for (int i = 0; i < 0x0100; i++) {
            for (int j = 0; j < 0x0c0; j++) {
                byte use = (byte) ((i % 4) > 1 ? 0x0FF : 0x00);
                mem.get(j)[i] = use;
            }
        }
    }

    public final void zeroAllRam() {
        // Format memory with FF FF 00 00 pattern
        for (int i = 0; i < 0x0100; i++) {
            blank.get(0)[i] = (byte) 0x0FF;
        }
        initMemoryPattern(mainMemory);
        if (getAuxMemory() != null) {
            initMemoryPattern(getAuxMemory());
        }
    }

    private final Semaphore configurationSemaphone = new Semaphore(1, true);

    /**
     *
     */
    @Override
    public void configureActiveMemory() {
        try {
            state = "";
            log("MMU Switches");
            configurationSemaphone.acquire();
            // First off, set up read/write for main memory (might get changed later on)
            if (SoftSwitches.RAMRD.getState()) {
                state = "Ra";
            } else {
                state = "R0";
            }
            activeRead.fillBanks(SoftSwitches.RAMRD.getState() ? getAuxMemory() : mainMemory);
            if (SoftSwitches.RAMWRT.getState()) {
                state += "Wa";
            } else {
                state += "W0";
            }
            activeWrite.fillBanks(SoftSwitches.RAMWRT.getState() ? getAuxMemory() : mainMemory);

            // Handle language card softswitches
            activeRead.fillBanks(rom);
            //activeRead.fillBanks(cPageRom);
            for (int i = 0x0c0; i < 0x0d0; i++) {
                activeWrite.set(i, null);
            }
            String LCR = "L0R";
            if (SoftSwitches.LCRAM.isOn()) {
                if (SoftSwitches.AUXZP.isOff()) {
                    LCR = "L1R";
                    activeRead.fillBanks(languageCard);
                    if (SoftSwitches.LCBANK1.isOff()) {
                        LCR = "L2R";
                        activeRead.fillBanks(languageCard2);
                    }
                } else {
                    activeRead.fillBanks(getAuxLanguageCard());
                    LCR = "L1aR";
                    if (SoftSwitches.LCBANK1.isOff()) {
                        LCR = "L2aR";
                        activeRead.fillBanks(getAuxLanguageCard2());
                    }
                }
            }

            String LCW = "L0W";
            if (SoftSwitches.LCWRITE.isOn()) {
                if (SoftSwitches.AUXZP.isOff()) {
                    LCW = "L1W";
                    activeWrite.fillBanks(languageCard);
                    if (SoftSwitches.LCBANK1.isOff()) {
                        LCW = "L2W";
                        activeWrite.fillBanks(languageCard2);
                    }
                } else {
                    activeWrite.fillBanks(getAuxLanguageCard());
                    LCW = "L1aW";
                    if (SoftSwitches.LCBANK1.isOff()) {
                        activeWrite.fillBanks(getAuxLanguageCard2());
                        LCW = "L2aW";
                    }
                }
            } else {
                // Make 0xd000 - 0xffff non-writable!
                for (int i = 0x0d0; i < 0x0100; i++) {
                    activeWrite.set(i, null);
                }
            }

            state += String.format(",%s,%s", LCR, LCW);
            // Handle 80STORE logic for bankswitching video ram
            if (SoftSwitches._80STORE.isOn()) {
                state += ",80S";
                if (SoftSwitches.PAGE2.isOn()) {
                    state += "2";
                }
                activeRead.setBanks(0x04, 0x04, 0x04,
                        SoftSwitches.PAGE2.isOn() ? getAuxMemory() : mainMemory);
                activeWrite.setBanks(0x04, 0x04, 0x04,
                        SoftSwitches.PAGE2.isOn() ? getAuxMemory() : mainMemory);
                if (SoftSwitches.HIRES.isOn()) {
                    state += "H";
                    activeRead.setBanks(0x020, 0x020, 0x020,
                            SoftSwitches.PAGE2.isOn() ? getAuxMemory() : mainMemory);
                    activeWrite.setBanks(0x020, 0x020, 0x020,
                            SoftSwitches.PAGE2.isOn() ? getAuxMemory() : mainMemory);
                }
            }

            // Handle zero-page bankswitching
            if (SoftSwitches.AUXZP.getState()) {
                state += ",Za";
                // Aux pages 0 and 1
                activeRead.setBanks(0, 2, 0, getAuxMemory());
                activeWrite.setBanks(0, 2, 0, getAuxMemory());
            } else {
                state += ",Z0";
                // Main pages 0 and 1
                activeRead.setBanks(0, 2, 0, mainMemory);
                activeWrite.setBanks(0, 2, 0, mainMemory);
            }

            /*
            INTCXROM   SLOTC3ROM  C1,C2,C4-CF   C3
            0         0          slot         rom
            0         1          slot         slot
            1         -          rom          rom
             */
            if (SoftSwitches.CXROM.getState()) {
                // Enable C1-CF to point to rom
                activeRead.setBanks(0, 0x0F, 0x0C1, cPageRom);
            } else {
                // Enable C1-CF to point to slots
                for (int slot = 1; slot <= 7; slot++) {
                    PagedMemory page = getCard(slot).map(Card::getCxRom).orElse(blank);
                    activeRead.setBanks(0, 1, 0x0c0 + slot, page);
                }
                if (getActiveSlot() == 0) {
                    for (int i = 0x0C8; i < 0x0D0; i++) {
                        activeRead.set(i, blank.get(0));
                    }
                } else {
                    getCard(getActiveSlot()).ifPresent(c -> activeRead.setBanks(0, 8, 0x0c8, c.getC8Rom()));
                }
                if (SoftSwitches.SLOTC3ROM.isOff()) {
                    // Enable C3 to point to internal ROM
                    activeRead.setBanks(2, 1, 0x0C3, cPageRom);
                    state += ",C30";
                }
                if (SoftSwitches.INTC8ROM.isOn()) {
                    state += ",C80";
                    // Enable C8-CF to point to internal ROM
                    activeRead.setBanks(7, 8, 0x0C8, cPageRom);
                } else {
                    state += String.format(",C8%d", getActiveSlot());
                }
            }
            // All ROM reads not intecepted will return 0xFF! (TODO: floating bus)
            activeRead.set(0x0c0, blank.get(0));
            configurationSemaphone.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(RAM128k.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void log(String message) {
        CPU cpu = computer.getCpu();
        if (cpu != null && cpu.isLogEnabled()) {
            String stack = "";
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                stack += e.getClassName() + "." + e.getMethodName() + "(" + e.getLineNumber() + ");";
            }
            cpu.log(stack);
            cpu.log(message + ";" + SoftSwitches.RAMRD + ";" + SoftSwitches.RAMWRT + ";" + SoftSwitches.AUXZP + ";" + SoftSwitches._80STORE + ";" + SoftSwitches.HIRES + ";" + SoftSwitches.PAGE2 + ";" + SoftSwitches.LCBANK1 + ";" + SoftSwitches.LCRAM + ";" + SoftSwitches.LCWRITE);
        }
    }

    /**
     *
     * @param path
     * @throws java.io.IOException
     */
    @Override
    protected void loadRom(String path) throws IOException {
        // Remap writable ram to reflect rom file structure
        byte[] ignore = new byte[256];
        activeWrite.set(0, ignore);  // Ignore first bank of data
        for (int i = 1; i < 17; i++) {
            activeWrite.set(i, ignore);
        }
        activeWrite.setBanks(0, cPageRom.getMemory().length, 0x011, cPageRom);
        activeWrite.setBanks(0, rom.getMemory().length, 0x020, rom);
        //----------------------
        InputStream inputRom = getClass().getClassLoader().getResourceAsStream(path);
        int read = 0;
        int addr = 0;
        byte[] in = new byte[1024];
        while (addr < 0x00FFFF && (read = inputRom.read(in)) > 0) {
            for (int i = 0; i < read; i++) {
                write(addr++, in[i], false, false);
            }
        }
//            System.out.println("Finished reading rom with " + inputRom.available() + " bytes left unread!");
        //dump();
        configureActiveMemory();
    }

    /**
     * @return the mainMemory
     */
    public PagedMemory getMainMemory() {
        return mainMemory;
    }

    abstract public PagedMemory getAuxVideoMemory();

    abstract public PagedMemory getAuxMemory();

    abstract public PagedMemory getAuxLanguageCard();

    abstract public PagedMemory getAuxLanguageCard2();

    /**
     * @return the languageCard
     */
    public PagedMemory getLanguageCard() {
        return languageCard;
    }

    /**
     * @return the languageCard2
     */
    public PagedMemory getLanguageCard2() {
        return languageCard2;
    }

    /**
     * @return the cPageRom
     */
    public PagedMemory getcPageRom() {
        return cPageRom;
    }

    /**
     * @return the rom
     */
    public PagedMemory getRom() {
        return rom;
    }

    void copyFrom(RAM128k currentMemory) {
        // This is really quick and dirty but should be sufficient to avoid most crashes...
        blank = currentMemory.blank;
        cPageRom = currentMemory.cPageRom;
        rom = currentMemory.rom;
        listeners = currentMemory.listeners;
        mainMemory = currentMemory.mainMemory;
        languageCard = currentMemory.languageCard;
        languageCard2 = currentMemory.languageCard2;
        cards = currentMemory.cards;
        activeSlot = currentMemory.activeSlot;
    }
}
