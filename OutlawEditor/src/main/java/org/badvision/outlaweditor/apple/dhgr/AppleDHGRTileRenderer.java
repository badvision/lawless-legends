package org.badvision.outlaweditor.apple.dhgr;

import org.badvision.outlaweditor.apple.*;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.badvision.outlaweditor.TileRenderer;
import static org.badvision.outlaweditor.apple.AppleNTSCGraphics.*;

/**
 *
 * @author brobert
 */
public class AppleDHGRTileRenderer extends AppleTileRenderer {
    @Override
    public WritableImage redrawSprite(byte[] spriteData, WritableImage img) {
        if (img == null) {
            img = new WritableImage(28, 32);
        }
        if (spriteData == null) return img;
        int[][] palette = useSolidPalette ? solidPalette : textPalette;
        for (int y = 0; y < 16; y++) {
//            int bleedOver = (spriteData[y * 2 + 1] & 128)==128? 256 : 0;
            int scan = spriteData[y * 4 + 3] & 255;
            scan <<=7;
            scan |= spriteData[y * 4 + 2] & 255;
            scan <<=7;
            scan |= spriteData[y * 4 + 1] & 255;
            scan <<=7;
            scan |= spriteData[y * 4] & 255;
            int last = (scan >> 26) & 3;
            int keep = scan & 0xff;
            scan <<= 2;
            scan |= last;
            for (int x = 0; x < 14; x++) {
                boolean isHiBit = ((spriteData[y * 2 + x / 7] & 128) != 0);
                
                int col1 = palette[ (x & 1) << 1][scan & 0x07f];
                Color color1 = Color.rgb(getRed(col1), getGreen(col1), getBlue(col1));
                scan >>= 1;
                if (x == 12) {
                    scan = scan & (isHiBit ? 0x07f : 0x01f) | (keep << 5);
                }
                int col2 = palette[ ((x & 1) << 1) + 1][scan & 0x07f];
                Color color2 = Color.rgb(getRed(col2), getGreen(col2), getBlue(col2));
                scan >>= 1;
                img.getPixelWriter().setColor(x * 2, y * 2, color1);
                img.getPixelWriter().setColor(x * 2, y * 2 + 1, color1);
                img.getPixelWriter().setColor(x * 2 + 1, y * 2, color2);
                img.getPixelWriter().setColor(x * 2 + 1, y * 2 + 1, color2);
            }
        }
        return img;
    }

    @Override
    public int getWidth() {
        return 28;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}