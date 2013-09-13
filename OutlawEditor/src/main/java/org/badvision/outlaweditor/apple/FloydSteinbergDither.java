package org.badvision.outlaweditor.apple;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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
public class FloydSteinbergDither {

    static final int totalPasses = 6;
    static final int nonErrorPasses = 1;

    public static interface DitherCallback {

        public void ditherCompleted(byte[] data);
    }

    public static void floydSteinbergDither(
            Image img,
            final org.badvision.outlaweditor.Platform platform,
            final int startX,
            final int startY,
            final int width,
            final int height,
            final byte[] screen,
            final int bufferWidth,
            final DitherCallback callback) {
        final AppleImageRenderer renderer = (AppleImageRenderer) platform.imageRenderer;
        final int errorWindow = 6;
        final int overlap = 2;
        final int pixelShift = -2;
        int byteRenderWidth = platform == org.badvision.outlaweditor.Platform.AppleII_DHGR ? 7 : 14;
        final WritableImage source = getScaledImage(img, width * byteRenderWidth, height);
        AnchorPane pane = new AnchorPane();
        Scene s = new Scene(pane);
        final ImageView previewImage = new ImageView(source);
        previewImage.setLayoutX(0);
        previewImage.setLayoutY(0);
        pane.getChildren().add(previewImage);
        final Text status = new Text("Status");
        status.setLayoutX(100);
        status.setLayoutY(40);
        status.setFont(Font.font("Arial", 18));
        status.setStroke(Color.BLACK);
        status.setEffect(new DropShadow(10.0, 0, 0, Color.WHITE));
        pane.getChildren().add(status);
        Stage progress = new Stage();
        progress.setScene(s);
        progress.show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final WritableImage keepScaled = new WritableImage(source.getPixelReader(), 560, 192);
                WritableImage tmpScaled = new WritableImage(source.getPixelReader(), 560, 192);
                for (int i = 0; i < screen.length; i++) {
                    screen[i] = (byte) Math.max(255, Math.random() * 256.0);
                }
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        previewImage.setCache(false);
//                        previewImage.setImage(keepScaled);
//                    }
//                });
//                try {
//                    while (previewImage.getImage() != keepScaled) {
//                        Thread.sleep(10);
//                    }
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(FloydSteinbergDither.class.getName()).log(Level.SEVERE, null, ex);
//                }
                int[] scanline = new int[3];
                List<Integer> pixels = new ArrayList<>();
                for (int pass = 0; pass < totalPasses; pass++) {
                    keepScaled.getPixelWriter().setPixels(0, 0, 560, 192, source.getPixelReader(), 0, 0);
                    tmpScaled.getPixelWriter().setPixels(0, 0, 560, 192, source.getPixelReader(), 0, 0);
                    final String statusText = "Pass: " + (pass + 1) + " of " + totalPasses;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            status.setText(statusText);
                        }
                    });
                    System.out.println("Image type: " + platform.name());
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x += 2) {
                            Thread.yield();
                            switch (platform) {
                                case AppleII:
                                    hiresDither(screen, y, x, scanline, pixels, tmpScaled, pass, keepScaled);
                                    break;
                                case AppleII_DHGR:
                                    doubleHiresDither(screen, y, x, scanline, pixels, tmpScaled, pass, keepScaled);
                                    break;
                            }
                        }
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            callback.ditherCompleted(screen);
                        }
                    });
                }
                status.setText("Complete!");
            }

            void hiresDither(final byte[] screen, int y, int x, int[] scanline, List<Integer> pixels, WritableImage tmpScaled, int pass, final WritableImage keepScaled) {
                int bb1 = screen[(y+startY) * bufferWidth + startX + x] & 255;
                int bb2 = screen[(y+startY) * bufferWidth + startX + x + 1] & 255;
                int next = bb2 & 127;  // Preserve hi-bit so last pixel stays solid, it is a very minor detail
                int prev = 0;
                if ((x+startX) > 0) {
                    prev = screen[(y+startY) * bufferWidth + startX + x - 1] & 255;
                }
                if ((x+startX) < 38) {
                    next = screen[(y+startY) * bufferWidth + startX + x + 2] & 255;
                }
                // First byte, compared with a sliding window encompassing the previous byte, if any.
                int leastError = Integer.MAX_VALUE;
                for (int hi = 0; hi < 2; hi++) {
                    int b1 = (hi << 7) | (bb1 & 0x07f);
                    int totalError = 0;
                    for (int c = 0; c < 7; c++) {
//                                for (int c = 6; c >= 0; c--) {
                        int on = b1 | (1 << c);
                        int off = on ^ (1 << c);
                        // get values for "off"
                        int i = hgrToDhgr[0][prev];
                        scanline[0] = i & 0x0fffffff;
                        i = hgrToDhgr[(i & 0x10000000) != 0 ? off | 0x0100 : off][bb2];
                        scanline[1] = i & 0x0fffffff;
//                                    scanline[2] = hgrToDhgr[(i & 0x10000000) != 0 ? next | 0x0100 : next][0] & 0x0fffffff;
                        int errorOff = getError(x * 14 - overlap + c * 2, y, 28 + c * 2 - overlap + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                        int off1 = pixels.get(c * 2 + 28 + pixelShift);
                        int off2 = pixels.get(c * 2 + 29 + pixelShift);
                        // get values for "on"
                        i = hgrToDhgr[0][prev];
                        scanline[0] = i & 0x0fffffff;
                        i = hgrToDhgr[(i & 0x10000000) != 0 ? on | 0x0100 : on][bb2];
                        scanline[1] = i & 0x0fffffff;
//                                    scanline[2] = hgrToDhgr[(i & 0x10000000) != 0 ? next | 0x0100 : next][0] & 0x0fffffff;
                        int errorOn = getError(x * 14 - overlap + c * 2, y, 28 + c * 2 - overlap + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                        int on1 = pixels.get(c * 2 + 28 + pixelShift);
                        int on2 = pixels.get(c * 2 + 29 + pixelShift);
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
                        if (pass >= nonErrorPasses) {
                            propagateError(x * 14 + c * 2, y, tmpScaled, col1, true, false);
                            propagateError(x * 14 + c * 2 + 1, y, tmpScaled, col2, false, true);
                        }
                    }
                    if (totalError < leastError) {
                        keepScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, tmpScaled.getPixelReader(), 0, y);
                        leastError = totalError;
                        bb1 = b1;
                    } else {
                        tmpScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, keepScaled.getPixelReader(), 0, y);
                    }
                }
                // Second byte, compared with a sliding window encompassing the next byte, if any.
                leastError = Integer.MAX_VALUE;
                for (int hi = 0; hi < 2; hi++) {
                    int b2 = (hi << 7) | (bb2 & 0x07f);
                    int totalError = 0;
                    for (int c = 0; c < 7; c++) {
//                                for (int c = 6; c >= 0; c--) {
                        int on = b2 | (1 << c);
                        int off = on ^ (1 << c);
                        // get values for "off"
                        int i = hgrToDhgr[bb1][off];
                        scanline[0] = i & 0xfffffff;
                        scanline[1] = hgrToDhgr[(i & 0x10000000) != 0 ? next | 0x0100 : next][0];
                        int errorOff = getError(x * 14 + 14 - overlap + c * 2, y, 14 - overlap + c * 2 + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                        int off1 = pixels.get(c * 2 + 14 + pixelShift);
                        int off2 = pixels.get(c * 2 + 15 + pixelShift);
                        // get values for "on"
                        i = hgrToDhgr[bb1][on];
                        scanline[0] = i & 0xfffffff;
                        scanline[1] = hgrToDhgr[(i & 0x10000000) != 0 ? next | 0x0100 : next][0];
                        int errorOn = getError(x * 14 + 14 - overlap + c * 2, y, 14 - overlap + c * 2 + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                        int on1 = pixels.get(c * 2 + 14 + pixelShift);
                        int on2 = pixels.get(c * 2 + 15 + pixelShift);
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
                        if (pass >= nonErrorPasses) {
                            propagateError(x * 14 + c * 2 + 14, y, tmpScaled, col1, true, false);
                            propagateError(x * 14 + c * 2 + 15, y, tmpScaled, col2, false, true);
                        }
                    }
                    if (totalError < leastError) {
                        keepScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, tmpScaled.getPixelReader(), 0, y);
                        leastError = totalError;
                        bb2 = b2;
                    } else {
                        tmpScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, keepScaled.getPixelReader(), 0, y);
                    }
                }
                screen[(y+startY) * bufferWidth + startX + x] = (byte) bb1;
                screen[(y+startY) * bufferWidth + startX + x + 1] = (byte) bb2;
            }

            void doubleHiresDither(final byte[] screen, int y, int x, int[] scanline, List<Integer> pixels, WritableImage tmpScaled, int pass, final WritableImage keepScaled) {
                if (x % 4 != 0) {
                    return;
                }
                scanline[0] = 0;
                if (x >= 4) {
                    scanline[0] = screen[y * 80 + x - 1] << 21;
                }
                scanline[1] = 0;
                if (x < 76) {
                    scanline[2] = screen[y * 80 + x + 4];
                }
                int bytes[] = new int[]{
                    screen[y * 80 + x] & 255,
                    screen[y * 80 + x + 1] & 255,
                    screen[y * 80 + x + 2] & 255,
                    screen[y * 80 + x + 3] & 255
                };

                for (int xx = 0; xx < 4; xx++) {
                    // First byte, compared with a sliding window encompassing the previous byte, if any.
                    int leastError = Integer.MAX_VALUE;
                        int b1 = (bytes[xx] & 0x07f);
                        for (int c = 0; c < 7; c++) {
                            int on = b1 | (1 << c);
                            int off = on ^ (1 << c);
                            // get values for "off"
                            int i = (xx == 3) ? off : bytes[3] & 255;
                            i <<= 7;
                            i |= (xx == 2) ? off : bytes[2] & 255;
                            i <<= 7;
                            i |= (xx == 1) ? off : bytes[1] & 255;
                            i <<= 7;
                            i |= (xx == 0) ? off : bytes[0] & 255;
                            scanline[1] = i;
                            int errorOff = getError((x + xx) * 7 - overlap + c, y, 28 + (xx * 7) + c - overlap + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                            int off1 = pixels.get(xx * 7 + c + 28 + pixelShift);
                            // get values for "on"
                            i = (xx == 3) ? on : bytes[3] & 255;
                            i <<= 7;
                            i |= (xx == 2) ? on : bytes[2] & 255;
                            i <<= 7;
                            i |= (xx == 1) ? on : bytes[1] & 255;
                            i <<= 7;
                            i |= (xx == 0) ? on : bytes[0] & 255;
                            scanline[1] = i;
                            int errorOn = getError((x + xx) * 7 - overlap + c, y, 28 + (xx * 7) + c - overlap + pixelShift, errorWindow, pixels, tmpScaled.getPixelReader(), scanline);
                            int on1 = pixels.get(xx * 7 + c + 28 + pixelShift);

                            int[] col1;
                            if (errorOff < errorOn) {
//                                totalError += errorOff;
                                b1 = off;
                                col1 = Palette.parseIntColor(off1);
                            } else {
//                                totalError += errorOn;
                                b1 = on;
                                col1 = Palette.parseIntColor(on1);
                            }
                            if (pass >= nonErrorPasses) {
                                propagateError((x + xx) * 7 + c, y, tmpScaled, col1, false, false);
                            }
                        }
//                        if (totalError < leastError) {
                            keepScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, tmpScaled.getPixelReader(), 0, y);
//                            leastError = totalError;
                            bytes[xx] = b1;
//                        } else {
//                            tmpScaled.getPixelWriter().setPixels(0, y, 560, (y < 191) ? 2 : 1, keepScaled.getPixelReader(), 0, y);
                }
                screen[(y+startY) * bufferWidth + startX + x] = (byte) bytes[0];
                screen[(y+startY) * bufferWidth + startX + x + 1] = (byte) bytes[1];
                screen[(y+startY) * bufferWidth + startX + x + 2] = (byte) bytes[2];
                screen[(y+startY) * bufferWidth + startX + x + 3] = (byte) bytes[3];
            }
        });
        t.start();
//        progress.close();
    }

    private static void propagateError(int x, int y, WritableImage img, int[] newColor, boolean propagateLeft, boolean propagateRight) {
        int solid = 255 << 24;
        for (int i = 0; i < 3; i++) {
            if (x + 1 < img.getWidth() && propagateRight) {
                int error = Palette.getComponent(img.getPixelReader().getArgb(x + 1, y), i) - newColor[i];
                int c = img.getPixelReader().getArgb(x + 1, y);
                img.getPixelWriter().setArgb(x + 1, y, solid | Palette.addError(c, i, (error * 7) >> 4));
            }
            if (y + 1 < img.getHeight()) {
                int error = Palette.getComponent(img.getPixelReader().getArgb(x, y), i) - newColor[i];
                if (x - 1 > 0 && propagateLeft) {
                    int c = img.getPixelReader().getArgb(x - 1, y + 1);
                    img.getPixelWriter().setArgb(x - 1, y + 1, solid | Palette.addError(c, i, (error * 3) >> 4));
                }
                int c = img.getPixelReader().getArgb(x, y + 1);
                img.getPixelWriter().setArgb(x, y + 1, solid | Palette.addError(c, i, (error * 5) >> 4));
                if (x + 1 < img.getWidth() && propagateRight) {
                    c = img.getPixelReader().getArgb(x + 1, y + 1);
                    img.getPixelWriter().setArgb(x + 1, y + 1, solid | Palette.addError(c, i, error >> 4));
                }
            }
        }
    }

    private static int getError(int imageXStart, int y, int scanlineXStart, int window, final List<Integer> pixels, PixelReader source, int[] scanline) {
        pixels.clear();
        PixelWriter fakeWriter = new PixelWriter() {
            @Override
            public PixelFormat getPixelFormat() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setArgb(int x, int y, int c) {
                pixels.add(c);
            }

            @Override
            public void setColor(int i, int i1, Color color) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public <T extends Buffer> void setPixels(int i, int i1, int i2, int i3, PixelFormat<T> pf, T t, int i4) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setPixels(int i, int i1, int i2, int i3, PixelFormat<ByteBuffer> pf, byte[] bytes, int i4, int i5) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setPixels(int i, int i1, int i2, int i3, PixelFormat<IntBuffer> pf, int[] ints, int i4, int i5) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setPixels(int i, int i1, int i2, int i3, PixelReader reader, int i4, int i5) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        AppleImageRenderer.renderScanline(fakeWriter, 0, scanline, true, false, 20);
        double max = 0;
        double min = Double.MAX_VALUE;
        double total = 0;
        List<Double> err = new ArrayList<>();
        for (int p = 0; p < window; p++) {
            if ((imageXStart + p) < 0 || (imageXStart + p) >= 560) {
                continue;
            }
            int[] c1 = Palette.parseIntColor(pixels.get(scanlineXStart + p));
            int[] c2 = Palette.parseIntColor(source.getArgb(imageXStart + p, y));
            double dist = Palette.distance(c1, c2);
            total += dist;
            max = Math.max(dist, max);
            min = Math.min(dist, min);
            err.add(dist);
        }
//        double avg = total/((double) window);
//        double range = max-min;
//        double totalDev = 0.0;
//        for (Double d : err) {
//            totalDev = Math.pow(d-avg, 2);
////            errorTotal += (d-min)/range;
//        }
//        totalDev /= ((double) window);
//        double stdDev = Math.sqrt(totalDev);
//        return (int) (min+(avg*(stdDev/range)));
        return (int) total;
    }

//                int currentPixel = source.getRGB(x, y);
//
//                for (int i = 0; i < 3; i++) {
//                    int error = Palette.getComponent(currentPixel, i) - Palette.getComponent(closestColor, i);
//                    if (x + 1 < source.getWidth()) {
//                        int c = source.getRGB(x + 1, y);
//                        source.setRGB(x + 1, y, Palette.addError(c, i, (error * 7) >> 4));
//                    }
//                    if (y + 1 < source.getHeight()) {
//                        if (x - 1 > 0) {
//                            int c = source.getRGB(x - 1, y + 1);
//                            source.setRGB(x - 1, y + 1, Palette.addError(c, i, (error * 3) >> 4));
//                        }
//                        {
//                            int c = source.getRGB(x, y + 1);
//                            source.setRGB(x, y + 1, Palette.addError(c, i, (error * 5) >> 4));
//                        }
//                        if (x + 1 < source.getWidth()) {
//                            int c = source.getRGB(x + 1, y + 1);
//                            source.setRGB(x + 1, y + 1, Palette.addError(c, i, error >> 4));
//                        }
//                    }
//                }
//            }
//        }
//        return dest;
//    }
    private static WritableImage getScaledImage(Image img, int width, int height) {
        Canvas c = new Canvas(width, height);
        c.getGraphicsContext2D().drawImage(img, 0, 0, width, height);
        WritableImage newImg = new WritableImage(width, height);
        SnapshotParameters sp = new SnapshotParameters();
        c.snapshot(sp, newImg);
        return newImg;
    }
}