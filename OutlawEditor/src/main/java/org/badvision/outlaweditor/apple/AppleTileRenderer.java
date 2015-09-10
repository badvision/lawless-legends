/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.apple;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.badvision.outlaweditor.TileRenderer;
import static org.badvision.outlaweditor.apple.AppleNTSCGraphics.*;

/**
 *
 * @author brobert
 */
public class AppleTileRenderer extends TileRenderer {

    public static boolean useSolidPalette = true;

    @Override
    public WritableImage redrawSprite(byte[] spriteData, WritableImage img, boolean useBleedOver) {
        if (img == null) {
            img = new WritableImage(28, 32);
        }
        if (spriteData == null) {
            return img;
        }
        int[][] palette = useSolidPalette ? solidPalette : textPalette;
        for (int y = 0; y < 16; y++) {
            int bleedOver = useBleedOver ? (spriteData[y * 2 + 1] & 192) == 192 ? 256 : 0 : 0;
            int scan = hgrToDhgr[bleedOver | (spriteData[y * 2] & 255)][spriteData[y * 2 + 1] & 255];
            int last = (scan >> 26) & 3;
            int keep = scan & 0xff;
            scan <<= 2;
            if (useBleedOver) {
                scan |= last;
            }
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
