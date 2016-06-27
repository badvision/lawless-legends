/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.apple.dhgr;

import org.badvision.outlaweditor.apple.*;
import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class AppleDHGRImageRenderer extends AppleImageRenderer {

    public static int BLACK = 0xff000000;
    public static int WHITE = 0xffffffff;
    // scanline is 20 16-bit words
    // If mixed-mode is used then useColor needs to be an 80-boolean array indicating which bytes are supposed to be BW

    @Override
    public byte[] renderPreview(TileMap map, int startX, int startY, int width, int height) {
        byte[] buffer = createImageBuffer(width, height);
        int pos = 0;
        int numRows = height / 16;
        int numCols = width / 4;
        for (int y = 0; y < numRows; y++) {
            for (int yy = 0; yy < 16; yy++) {
                for (int x = 0; x < numCols; x++) {
                    Tile t = map.get(x + startX, y + startY);
                    if (t == null) {
                        buffer[pos++] = 0;
                        buffer[pos++] = 0;
                        buffer[pos++] = 0;
                        buffer[pos++] = 0;
                    } else {
                        byte[] tileData = TileUtils.getPlatformData(t, Platform.AppleII_DHGR);
                        buffer[pos++] = tileData[yy * 2];
                        buffer[pos++] = tileData[yy * 2 + 1];
                        buffer[pos++] = tileData[yy * 2 + 2];
                        buffer[pos++] = tileData[yy * 2 + 3];
                    }
                }
            }
        }
        return buffer;
    }

    @Override
    public WritableImage renderImage(WritableImage img, byte[] rawImage, int width, int height) {
        if (img == null) {
            img = new WritableImage(width * 7, height * 2);
        }
        for (int y = 0; y < height; y++) {
            renderScanline(img, y, width, rawImage);
        }
        return img;
    }

    @Override
    public WritableImage renderScanline(WritableImage img, int y, int width, byte[] rawImage) {
        if (y < 0) return img;
        int[] scanline = new int[width/4];
        for (int x = 0; x < width; x += 4) {
            int scan = rawImage[y * width + x + 3] & 255;
            scan <<=7;
            scan |= rawImage[y * width + x + 2] & 255;
            scan <<=7;
            scan |= rawImage[y * width + x + 1] & 255;
            scan <<=7;
            scan |= rawImage[y * width + x] & 255;
            scanline[x / 4] = scan;
        }
        renderScanline(img.getPixelWriter(), y * 2, scanline, true, false, width);
        renderScanline(img.getPixelWriter(), y * 2 + 1, scanline, true, false, width);
        return img;
    }
//    
//    public static void renderScanline(PixelWriter img, int y, int[] scanline, boolean hiresMode, boolean mixedMode, boolean... useColor) {
//        AppleImageRenderer.renderScanline(img, y, scanline, hiresMode, mixedMode, useColor);
//    }
}
