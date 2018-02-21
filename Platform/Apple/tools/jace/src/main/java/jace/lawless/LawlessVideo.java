package jace.lawless;

import jace.Emulator;
import jace.LawlessLegends;
import jace.apple2e.RAM128k;
import jace.apple2e.VideoNTSC;
import jace.core.Computer;
import jace.core.PagedMemory;
import java.util.Arrays;
import javafx.scene.image.WritableImage;

/**
 * Lawless-enhanced video output for readable text
 */
public class LawlessVideo extends VideoNTSC {

    private static RenderEngine activeEngine = RenderEngine.UNKNOWN;
    private boolean invActive = false;
    private boolean titleScreen = false;
    private boolean[][] activeMask = new boolean[192][80];
    
    
    public static enum RenderEngine {
        _2D(new int[]{
            9, 8, 34, 17,
            44, 24, 76, 136,
            44, 143, 76, 184
        }),
        _3D(new int[]{
            9, 8, 34, 17,
            44, 24, 76, 136,
            44, 143, 76, 184,
            8, 172, 14, 182,}),
        INVENTORY(new int[]{
            2, 6, 78, 186
        }),
        PORTRAIT, UNKNOWN;
        boolean[][] colorMask;

        RenderEngine(int[] mask) {
            this();
            for (int i = 0; i < mask.length; i += 4) {
                int x1 = mask[i],
                        y1 = mask[i + 1],
                        x2 = mask[i + 2],
                        y2 = mask[i + 3];
                for (int y = y1; y < y2; y++) {
                    for (int x = x1; x < x2; x++) {
                        colorMask[y][x] = false;
                    }
                }
            }
        }

        RenderEngine() {
            colorMask = new boolean[192][80];
            for (int y = 0; y < 192; y++) {
                colorMask[y] = new boolean[80];
                Arrays.fill(colorMask[y], true);
            }
        }
    };

    public LawlessVideo(Computer computer) {
        super(computer);
    }

    public void setEngine(RenderEngine e) {
        activeEngine = e;
        for (int y=0; y < 192; y++) {
            System.arraycopy(e.colorMask[y], 0, activeMask[y], 0, 80);
        }
        Emulator.computer.onNextVBL(() -> Emulator.computer.getVideo().forceRefresh());
        System.out.println("Detected engine: " + e.name());
    }
    
    public void setBWFlag(int addr, boolean b) {
        addr &= 0x01FFF;
        int row = VideoNTSC.identifyHiresRow(addr);
        if (row < 0 || row > 192) {
            return;
        }
        int col = addr - VideoNTSC.calculateHiresOffset(row);
        if (row > 20 && row < 136 && col < 20) {
            boolean prev = activeMask[row][col*2];
            activeMask[row][col*2] = b;
            activeMask[row][col*2+1] = b;
            if (prev ^ b) {
                redraw();
            }
        }
    }

    static public int[] divBy56 = new int[560];

    static {
        for (int i = 0; i < 560; i++) {
            divBy56[i] = i / 56;
        }
    }

    @Override
    public void vblankStart() {
        super.vblankStart();
        // Row 5 = Black
        int row4 = getSummary(4);
        int row5 = getSummary(5);
        int row6 = getSummary(6);
        int row7 = getSummary(7);
        // Rows 6,7 = White
        invActive = row5 == 0
                && row6 == 1270
                && row7 == 1270;
        titleScreen = row4 == 828 && row5 == 513 && row6 == 382;
    }

    public int getSummary(int row) {
        PagedMemory mainMemory = ((RAM128k) computer.getMemory()).getMainMemory();
        int rowAddr = getCurrentWriter().getYOffset(row);
        int total = 0;
        for (int i = rowAddr + 4; i < rowAddr + 14; i++) {
            total += mainMemory.readByte(i) & 0x07f;
        }
        return total;
    }

    @Override
    public void hblankStart(WritableImage screen, int y, boolean isDirty) {
        int rowStart = getCurrentWriter().getYOffset(y);
        if (rowStart >= 0x02000) {
            boolean[] color = activeMask[y];
            if (titleScreen) {
                color = RenderEngine.UNKNOWN.colorMask[y];
            } else if (invActive) {
                color = RenderEngine.INVENTORY.colorMask[y];
            } else if (activeEngine == RenderEngine.PORTRAIT) {
                color = RenderEngine._2D.colorMask[y];
            }
            System.arraycopy(color, 0, colorActive, 0, 80);
        }
        super.hblankStart(screen, y, isDirty); //To change body of generated methods, choose Tools | Templates.
    }
}
