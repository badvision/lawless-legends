package jace.lawless;

import jace.Emulator;
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
    private boolean titleScreen = true;
    private boolean[][] activeMask = new boolean[192][80];
    
    
    public static enum RenderEngine {
        FULL_COLOR,
        FULL_TEXT(new int[]{
            2, 6, 78, 186            
        }),
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
        MAP(new int[]{
            2, 6, 78, 11,
            2, 11, 4, 186,
            76, 11, 78, 186,            
            2, 182, 78, 186,
            28, 3, 52, 6
        }), 
        UNKNOWN;
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
        if (activeEngine != e) {
            titleScreen = false;
            activeEngine = e;
            for (int y=0; y < 192; y++) {
                System.arraycopy(activeEngine.colorMask[y], 0, activeMask[y], 0, 80);
            }
            Emulator.computer.onNextVBL(() -> Emulator.computer.getVideo().forceRefresh());
            System.out.println("Detected engine: " + e.name());
        } else {
            System.out.println("Detected engine same as before: " + e.name());            
        }
    }
    
    public void setBWFlag(int addr, boolean b) {
        addr &= 0x01FFF;
        int row = VideoNTSC.identifyHiresRow(addr);
        if (row < 0 || row > 192) {
            return;
        }
        int col = addr - VideoNTSC.calculateHiresOffset(row);
        /*
        if (row > 20 && row < 136 && col < 20) {
            boolean prev = activeMask[row][col*2];
            activeMask[row][col*2] = b;
            activeMask[row][col*2+1] = b;
            if (prev ^ b) {
                redraw();
            }
        }
        */
    }

    static public int[] divBy56 = new int[560];

    static {
        for (int i = 0; i < 560; i++) {
            divBy56[i] = i / 56;
        }
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
                color = RenderEngine.FULL_COLOR.colorMask[y];
            }
            System.arraycopy(color, 0, colorActive, 0, 80);
        }
        super.hblankStart(screen, y, isDirty);
    }
}
