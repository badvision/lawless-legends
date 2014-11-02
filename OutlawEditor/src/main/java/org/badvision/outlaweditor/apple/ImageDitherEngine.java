package org.badvision.outlaweditor.apple;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.badvision.outlaweditor.Platform;
import static org.badvision.outlaweditor.apple.AppleNTSCGraphics.hgrToDhgr;

/* Copyright (c) 2013 the authors listed at the following URL, and/or
 the authors of referenced articles or incorporated external code:
 http://en.literateprograms.org/Floyd-Steinberg_dithering_(Java)?action=history&offset=20080201121723

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Retrieved from: http://en.literateprograms.org/Floyd-Steinberg_dithering_(Java)?oldid=12476
 * Original code by Spoon! (Feb 2008)
 * Modified and adapted to work with Apple Game Server by Brendan Robert (2013)
 * Some of the original code of this class was migrated over to the Palette class which already manages colors in AGS.
 */
public class ImageDitherEngine {

    int byteRenderWidth;
    int pixelRenderWidth;
    final int errorWindow = 6;
    final int overlap = 3;
    final int pixelShiftHgr = -2;
    final int pixelShiftDhgr = -2;
    WritableImage source;
    byte[] screen;
    Platform platform;
    int bufferWidth;
    int height;
    int divisor;
    public int[][] coefficients;

    public ImageDitherEngine(Platform platform) {
        this.platform = platform;
        byteRenderWidth = platform == Platform.AppleII_DHGR ? 7 : 14;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setSourceImage(Image img) {
        source = getScaledImage(img, pixelRenderWidth, height);
    }

    private static WritableImage getScaledImage(Image img, int width, int height) {
        Canvas c = new Canvas(width, height);
        c.getGraphicsContext2D().drawImage(img, 0, 0, width, height);
        WritableImage newImg = new WritableImage(width, height);
        SnapshotParameters sp = new SnapshotParameters();
        c.snapshot(sp, newImg);
        return newImg;
    }

    public WritableImage getPreviewImage() {
        WritableImage out = new WritableImage(pixelRenderWidth, height * 2);
        platform.imageRenderer.renderImage(out, screen, bufferWidth, height);
        return out;
    }

    public void setOutputDimensions(int width, int height) {
        this.bufferWidth = width;
        this.pixelRenderWidth = width * byteRenderWidth;
        this.height = height;
        screen = platform.imageRenderer.createImageBuffer(width, height);
    }

    public void setDivisor(int divisor) {
        this.divisor = divisor;
    }

    public void setCoefficients(int[][] coefficients) {
        this.coefficients = coefficients;
    }

    int startX;
    int startY;

    public void setTargetCoordinates(int x, int y) {
        startX = x;
        startY = y;
    }

    WritableImage keepScaled;
    WritableImage tmpScaled1;
    WritableImage tmpScaled2;
    int[] scanline;
    List<Integer> pixels;

    public Image getScratchBuffer() {
        return keepScaled;
    }

    public byte[] dither(boolean propagateError) {
        keepScaled = new WritableImage(source.getPixelReader(), pixelRenderWidth, height);
        tmpScaled1 = new WritableImage(source.getPixelReader(), pixelRenderWidth, height);
        tmpScaled2 = new WritableImage(source.getPixelReader(), pixelRenderWidth, height);
        for (int i = 0; i < screen.length; i++) {
            screen[i] = (byte) 0;
        }
        scanline = new int[3];
        pixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < bufferWidth; x += 2) {
                switch (platform) {
                    case AppleII:
                        hiresDither(y, x, propagateError);
                        break;
                    case AppleII_DHGR:
                        doubleHiresDither(y, x, propagateError);
                        break;
                }
            }
        }
        return screen;
    }

    void hiresDither(int y, int x, boolean propagateError) {
        int ditherVerticalRange = Math.min(3, height - y);
        int bb1 = screen[(y + startY) * bufferWidth + startX + x] & 255;
        int bb2 = screen[(y + startY) * bufferWidth + startX + x + 1] & 255;
        int next = bb2 & 127;  // Preserve hi-bit so last pixel stays solid, it is a very minor detail
        int prev = 0;
        if ((x + startX) > 0) {
            prev = screen[(y + startY) * bufferWidth + startX + x - 1] & 255;
        }
        if ((x + startX) < (bufferWidth - 2)) {
            next = screen[(y + startY) * bufferWidth + startX + x + 2] & 255;
        }
        // First byte, compared with a sliding window encompassing the previous byte, if any.
        long leastError = Long.MAX_VALUE;
        for (int hi = 0; hi < 2; hi++) {
            tmpScaled2.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, keepScaled.getPixelReader(), 0, y);
            int b1 = (hi << 7);
            long totalError = 0;
            for (int c = 0; c < 7; c++) {
                int on = b1 | (1 << c);
                int off = on ^ (1 << c);
                // get values for "off"
                int i = hgrToDhgr[0][prev];
                scanline[0] = i;
                i = hgrToDhgr[(i & 0x010000000) >> 20 | off][bb2];
                scanline[1] = i;
                double errorOff = getError(x * 14 - overlap + c * 2, y, 28 + c * 2 - overlap + pixelShiftHgr, errorWindow, tmpScaled2.getPixelReader(), scanline);
                int off1 = pixels.get(c * 2 + 28 + pixelShiftHgr);
                int off2 = pixels.get(c * 2 + 29 + pixelShiftHgr);
                // get values for "on"
                i = hgrToDhgr[0][prev];
                scanline[0] = i;
                i = hgrToDhgr[(i & 0x010000000) >> 20 | on][bb2];
                scanline[1] = i;
                double errorOn = getError(x * 14 - overlap + c * 2, y, 28 + c * 2 - overlap + pixelShiftHgr, errorWindow, tmpScaled2.getPixelReader(), scanline);
                int on1 = pixels.get(c * 2 + 28 + pixelShiftHgr);
                int on2 = pixels.get(c * 2 + 29 + pixelShiftHgr);
                int[] col1;
                int[] col2;
                if (errorOff < errorOn) {
                    totalError += errorOff;
                    b1 = off;
                    col1 = Palette.parseIntColor(off1);
                    col2 = Palette.parseIntColor(off2);
                } else {
                    totalError += errorOn;
                    b1 = on;
                    col1 = Palette.parseIntColor(on1);
                    col2 = Palette.parseIntColor(on2);
                }
                if (propagateError) {
                    propagateError(x * 14 + c * 2 + pixelShiftHgr, y, tmpScaled2, col1);
                    propagateError(x * 14 + c * 2 + 1 + pixelShiftHgr, y, tmpScaled2, col2);
                }
            }
            if (totalError < leastError) {
                tmpScaled1.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, tmpScaled2.getPixelReader(), 0, y);
                leastError = totalError;
                bb1 = b1;
            }
        }
        keepScaled.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, tmpScaled1.getPixelReader(), 0, y);
        // Second byte, compared with a sliding window encompassing the next byte, if any.
        leastError = Long.MAX_VALUE;
        for (int hi = 0; hi < 2; hi++) {
            tmpScaled2.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, keepScaled.getPixelReader(), 0, y);
            int b2 = (hi << 7);
            long totalError = 0;
            for (int c = 0; c < 7; c++) {
                int on = b2 | (1 << c);
                int off = on ^ (1 << c);
                // get values for "off"
                int i = hgrToDhgr[bb1][off];
                scanline[0] = i;
                scanline[1] = hgrToDhgr[(i & 0x010000000) >> 20 | next][0];
                double errorOff = getError(x * 14 + 14 - overlap + c * 2, y, 14 + c * 2 - overlap + pixelShiftHgr, errorWindow, tmpScaled2.getPixelReader(), scanline);
                int off1 = pixels.get(c * 2 + 14 + pixelShiftHgr);
                int off2 = pixels.get(c * 2 + 15 + pixelShiftHgr);
                // get values for "on"
                i = hgrToDhgr[bb1][on];
                scanline[0] = i;
                scanline[1] = hgrToDhgr[(i & 0x010000000) >> 20 | next][0];
                double errorOn = getError(x * 14 + 14 - overlap + c * 2, y, 14 + c * 2 - overlap + pixelShiftHgr, errorWindow, tmpScaled2.getPixelReader(), scanline);
                int on1 = pixels.get(c * 2 + 14 + pixelShiftHgr);
                int on2 = pixels.get(c * 2 + 15 + pixelShiftHgr);
                int[] col1;
                int[] col2;
                if (errorOff < errorOn) {
                    totalError += errorOff;
                    b2 = off;
                    col1 = Palette.parseIntColor(off1);
                    col2 = Palette.parseIntColor(off2);
                } else {
                    totalError += errorOn;
                    b2 = on;
                    col1 = Palette.parseIntColor(on1);
                    col2 = Palette.parseIntColor(on2);
                }
                if (propagateError) {
                    propagateError(x * 14 + c * 2 + 14 + pixelShiftHgr, y, tmpScaled2, col1);
                    propagateError(x * 14 + c * 2 + 15 + pixelShiftHgr, y, tmpScaled2, col2);
                }
            }
            if (totalError < leastError) {
                tmpScaled1.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, tmpScaled2.getPixelReader(), 0, y);
                leastError = totalError;
                bb2 = b2;
            }
        }
        keepScaled.getPixelWriter().setPixels(0, y, pixelRenderWidth, ditherVerticalRange, tmpScaled1.getPixelReader(), 0, y);
        screen[(y + startY) * bufferWidth + startX + x] = (byte) bb1;
        screen[(y + startY) * bufferWidth + startX + x + 1] = (byte) bb2;
    }

    void doubleHiresDither(int y, int x, boolean propagateError) {
        if (x % 4 != 0) {
            return;
        }
        scanline[0] = 0;
        if (x >= 4) {
            scanline[0] = screen[y * bufferWidth + x - 1] << 21;
        }
        scanline[1] = 0;
        scanline[2] = 0;
        if (x + 4 < bufferWidth) {
            scanline[2] = screen[y * bufferWidth + x + 4];
        }
        int bytes[] = new int[]{
            screen[y * bufferWidth + x] & 255,
            screen[y * bufferWidth + x + 1] & 255,
            screen[y * bufferWidth + x + 2] & 255,
            screen[y * bufferWidth + x + 3] & 255
        };

        for (int byteOffset = 0; byteOffset < 4; byteOffset++) {
            // First byte, compared with a sliding window encompassing the previous byte, if any.
            int b1 = (bytes[byteOffset] & 0x07f);
            for (int bit = 0; bit < 7; bit++) {
                int on = b1 | (1 << bit);
                int off = on ^ (1 << bit);
                // get values for "off"
                int i = (byteOffset == 3) ? off : bytes[3] & 255;
                i <<= 7;
                i |= (byteOffset == 2) ? off : bytes[2] & 255;
                i <<= 7;
                i |= (byteOffset == 1) ? off : bytes[1] & 255;
                i <<= 7;
                i |= (byteOffset == 0) ? off : bytes[0] & 255;
                scanline[1] = i;
                double errorOff = getError((x + byteOffset) * 7 - overlap + bit, y, 28 + (byteOffset * 7) + bit - overlap + pixelShiftDhgr, errorWindow, keepScaled.getPixelReader(), scanline);
                int offColor = pixels.get(byteOffset * 7 + bit + 28+ pixelShiftDhgr);
                // get values for "on"
                i = (byteOffset == 3) ? on : bytes[3] & 255;
                i <<= 7;
                i |= (byteOffset == 2) ? on : bytes[2] & 255;
                i <<= 7;
                i |= (byteOffset == 1) ? on : bytes[1] & 255;
                i <<= 7;
                i |= (byteOffset == 0) ? on : bytes[0] & 255;
                scanline[1] = i;
                double errorOn = getError((x + byteOffset) * 7 - overlap + bit, y, 28 + (byteOffset * 7) + bit - overlap + pixelShiftDhgr, errorWindow, keepScaled.getPixelReader(), scanline);
                int onColor = pixels.get(byteOffset * 7 + bit + 28+ pixelShiftDhgr);

                int[] col1;
                if (errorOff < errorOn) {
                    b1 = off;
                    col1 = Palette.parseIntColor(offColor);
                } else {
                    b1 = on;
                    col1 = Palette.parseIntColor(onColor);
                }
                if (propagateError) {
                    propagateError((x + byteOffset) * 7 + bit, y, keepScaled, col1);
                }
            }
            bytes[byteOffset] = b1;
            screen[(y + startY) * bufferWidth + startX + x] = (byte) bytes[0];
            screen[(y + startY) * bufferWidth + startX + x + 1] = (byte) bytes[1];
            screen[(y + startY) * bufferWidth + startX + x + 2] = (byte) bytes[2];
            screen[(y + startY) * bufferWidth + startX + x + 3] = (byte) bytes[3];
        }
    }

    public static int ALPHA_SOLID = 255 << 24;

    private void propagateError(int x, int y, WritableImage img, int[] newColor) {
        if (x < 0 || y < 0) {
            return;
        }
        int pixel = img.getPixelReader().getArgb(x, y);
        for (int i = 0; i < 3; i++) {
            int error = Palette.getComponent(pixel, i) - newColor[i];
            for (int yy = 0; yy < 3 && y + yy < img.getHeight(); yy++) {
                for (int xx = -2; xx < 3 && x + xx < img.getWidth(); xx++) {
                    if (x + xx < 0 || coefficients[xx + 2][yy] == 0) {
                        continue;
                    }
                    int c = img.getPixelReader().getArgb(x + xx, y + yy);
                    int errorAmount = ((error * coefficients[xx + 2][yy]) / divisor);
                    img.getPixelWriter().setArgb(x + xx, y + yy, ALPHA_SOLID | Palette.addError(c, i, errorAmount));
                }
            }
        }
    }

    PixelWriter fakeWriter = new PixelWriter() {
        @Override
        public PixelFormat getPixelFormat() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setArgb(int x, int y, int c) {
            pixels.add(c);
        }

        @Override
        public void setColor(int i, int i1, Color color) {
        }

        @Override
        public <T extends Buffer> void setPixels(int i, int i1, int i2, int i3, PixelFormat<T> pf, T t, int i4) {
        }

        @Override
        public void setPixels(int i, int i1, int i2, int i3, PixelFormat<ByteBuffer> pf, byte[] bytes, int i4, int i5) {
        }

        @Override
        public void setPixels(int i, int i1, int i2, int i3, PixelFormat<IntBuffer> pf, int[] ints, int i4, int i5) {
        }

        @Override
        public void setPixels(int i, int i1, int i2, int i3, PixelReader reader, int i4, int i5) {
        }
    };

    private double getError(int imageXStart, int y, int scanlineXStart, int window, PixelReader source, int[] scanline) {
        pixels.clear();
        AppleImageRenderer.renderScanline(fakeWriter, 0, scanline, true, false, 20);
        double total = 0;
        for (int p = 0; p < window; p++) {
            if ((imageXStart + p) < 0 || (imageXStart + p) >= pixelRenderWidth || scanlineXStart + p < 0) {
                continue;
            }
            int[] c1 = Palette.parseIntColor(pixels.get(scanlineXStart + p));
            int[] c2 = Palette.parseIntColor(source.getArgb(imageXStart + p, y));
            double dist = Palette.distance(c1, c2);
            total += dist;
        }
        return (int) total;
    }
}
