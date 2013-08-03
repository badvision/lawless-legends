/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.apple;

import javafx.scene.image.PixelWriter;
import static org.badvision.outlaweditor.apple.AppleNTSCGraphics.*;
import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.ImageRenderer;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class AppleImageRenderer extends ImageRenderer {

    public static int BLACK = 0xff000000;
    public static int WHITE = 0xffffffff;
    // scanline is 20 16-bit words
    // If mixed-mode is used then useColor needs to be an 80-boolean array indicating which bytes are supposed to be BW

    @Override
    public byte[] createImageBuffer() {
        return new byte[40 * 192];
    }

    @Override
    public WritableImage renderPreview(TileMap map, int startX, int startY) {
        byte[] buffer = createImageBuffer();
        int pos = 0;
        for (int y = 0; y < 12; y++) {
            for (int yy = 0; yy < 16; yy++) {
                for (int x = 0; x < 20; x++) {
                    Tile t = map.get(x + startX, y + startY);
                    if (t == null) {
                        buffer[pos++] = 0;
                        buffer[pos++] = 0;
                    } else {
                        byte[] tileData = TileUtils.getPlatformData(t, Platform.AppleII);
                        buffer[pos++] = tileData[yy * 2];
                        buffer[pos++] = tileData[yy * 2 + 1];
                    }
                }
            }
        }
        return renderImage(null, buffer);
    }

    @Override
    public WritableImage renderImage(WritableImage img, byte[] rawImage) {
        if (img == null) {
            img = new WritableImage(560, 384);
        }
        for (int y = 0; y < 192; y++) {
            renderScanline(img, y, rawImage);
        }
        return img;
    }

    @Override
    public WritableImage renderScanline(WritableImage img, int y, byte[] rawImage) {
        int[] scanline = new int[20];
        boolean extraHalfBit = false;
        for (int x = 0; x < 40; x += 2) {
            int b1 = rawImage[y * 40 + x] & 255;
            int b2 = rawImage[y * 40 + x + 1] & 255;
            int i = hgrToDhgr[(extraHalfBit && x > 0) ? b1 | 0x0100 : b1][b2];
            extraHalfBit = (i & 0x10000000) != 0;
            scanline[x / 2] = i & 0xfffffff;
        }
        renderScanline(img.getPixelWriter(), y * 2, scanline, true, false);
        renderScanline(img.getPixelWriter(), y * 2 + 1, scanline, true, false);
        return img;
    }
    
    public static void renderScanline(PixelWriter img, int y, int[] scanline, boolean hiresMode, boolean mixedMode, boolean... useColor) {
        int[][] activePalette = AppleTileRenderer.useSolidPalette ? solidPalette : textPalette;
        int byteCounter = 0;
        int x = 0;
        for (int s = 0; s < scanline.length; s++) {
            int add = 0;
            int bits = 0;
            if (hiresMode) {
                bits = scanline[s] << 2;
                if (s > 0) {
                    bits |= (scanline[s - 1] >> 26) & 3;
                }
            } else {
                bits = scanline[s] << 3;
                if (s > 0) {
                    bits |= (scanline[s - 1] >> 25) & 7;
                }
            }
            if (s < scanline.length - 1) {
                add = (scanline[s + 1] & 7);
            }
            boolean isBW = false;
            if (mixedMode) {
                for (int i = 0; i < 28; i++) {
                    if (i % 7 == 0) {
                        isBW = !hiresMode && !useColor[byteCounter];
                        byteCounter++;
                    }
                    try {
                        if (isBW) {
                            img.setArgb(x++, y, ((bits & 0x8) == 0) ? BLACK : WHITE);
                        } else {
                            img.setArgb(x++, y, activePalette[i % 4][bits & 0x07f]);
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        // Ignore
                    }
                    bits >>= 1;
                    if (i == 20) {
                        bits |= add << (hiresMode ? 9 : 10);
                    }
                }
            } else {
                for (int i = 0; i < 28; i++) {
                    try {
                        img.setArgb(x++, y, activePalette[i % 4][bits & 0x07f]);
                    } catch (IndexOutOfBoundsException ex) {
                        // Ignore
                    }
                    bits >>= 1;
                    if (i == 20) {
                        bits |= add << (hiresMode ? 9 : 10);
                    }
                }
            }
        }
    }
}
