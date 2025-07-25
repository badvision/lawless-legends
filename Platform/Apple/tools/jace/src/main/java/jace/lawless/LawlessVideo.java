package jace.lawless;

import java.util.Arrays;

import jace.apple2e.RAM128k;
import jace.apple2e.VideoNTSC;
import jace.core.PagedMemory;
import javafx.scene.image.WritableImage;

/**
 * Lawless-enhanced video output for readable text
 */
public class LawlessVideo extends VideoNTSC {
    public final boolean[][] activeMask = new boolean[192][80];


    public LawlessVideo() {
        super();
        this.vblankStart();
        for (boolean[] row : activeMask) {
            Arrays.fill(row, true);
        }
    }

    static public int[] divBy56 = new int[560];

    static {
        for (int i = 0; i < 560; i++) {
            divBy56[i] = i / 56;
        }
    }
    public int getSummary(int row) {
        PagedMemory mainMemory = ((RAM128k) getMemory()).getMainMemory();
        int rowAddr = getCurrentWriter().getYOffset(row);
        int total = 0;
        for (int i = rowAddr + 4; i < rowAddr + 14; i++) {
            total += mainMemory.readByte(i) & 0x07f;
        }
        return total;
    }

    @Override
    public void hblankStart(WritableImage screen, int y, boolean isDirty) {
        if (y < 0 || y > 192) {
            return;
        }
        int rowStart = getCurrentWriter().getYOffset(y);
        if (rowStart >= 0x02000) {
            System.arraycopy(activeMask[y], 0, colorActive, 0, 80);
        }
        super.hblankStart(screen, y, isDirty);
    }
}
