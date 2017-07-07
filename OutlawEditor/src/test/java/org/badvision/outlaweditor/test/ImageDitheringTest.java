/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.test;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.badvision.outlaweditor.apple.AppleTileRenderer;
import org.badvision.outlaweditor.apple.ImageDitherEngine;
import org.badvision.outlaweditor.apple.Palette;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;

/**
 *
 * @author blurry
 */
public class ImageDitheringTest {

    public ImageDitheringTest() {
    }

    ImageDitherEngine hgrDither, dhgrDither;

    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        AppleTileRenderer.useSolidPalette = false;
        hgrDither = new ImageDitherEngine(org.badvision.outlaweditor.api.Platform.AppleII);
        hgrDither.setOutputDimensions(40, 192);
        dhgrDither = new ImageDitherEngine(org.badvision.outlaweditor.api.Platform.AppleII_DHGR);
        dhgrDither.setOutputDimensions(80, 192);
        
    }

    @After
    public void tearDown() {
    }

    @Test
    public void colorDiffTest() {
        int[] white = {255, 255, 255};
        int[] black = {0, 0, 0};
        int[] gray = {128, 128, 128};
        assertEquals(0, Palette.distance(white, white), 0);
        assertEquals(0, Palette.distance(black, black), 0);
        assertEquals(0, Palette.distance(gray, gray), 0);
        double midDist1 = Palette.distance(white, gray);
        double midDist2 = Palette.distance(black, gray);
        assertEquals(midDist1, midDist2, 3.0);
        double maxDist = Palette.distance(white, black);
        assertEquals(maxDist, midDist1 + midDist2, 0.01);
        assertEquals(255.0 * Math.sqrt(3.0), maxDist, 0.01);
    }

     @Test
     public void blackTestHGR() {
         testSolidColor(Color.BLACK, hgrDither, 0.0);
     }
     @Test
     public void blackTestDHGR() {
         testSolidColor(Color.BLACK, dhgrDither, 0.0);
     }
     @Test
     public void whiteTestHGR() {
         testSolidColor(Color.WHITE, hgrDither, 3.0);
     }
     @Test
     public void whiteTestDHGR() {
         testSolidColor(Color.WHITE, dhgrDither, 3.0);
     }

    @Test
    public void grayHGRTest() {
        testSolidColor(new Color(0.5f, 0.5f, 0.5f, 1.0f), hgrDither, 100.0);
    }
    @Test
    public void grayDHGRTest() {
        testSolidColor(new Color(0.5f, 0.5f, 0.5f, 1.0f), dhgrDither, 100.0);
    }
    @Test
    public void redHGRTest() {
        testSolidColor(new Color(1f, 0f, 0f, 1.0f), hgrDither, 115.0);
    }
    @Test
    public void redDHGRTest() {
        testSolidColor(new Color(1f, 0f, 0f, 1.0f), dhgrDither, 100.0);
    }
    @Test
    public void greenHGRTest() {
        testSolidColor(new Color(0f, 1f, 0f, 1.0f), hgrDither, 100.0);
    }
    @Test
    public void greenDHGRTest() {
        testSolidColor(new Color(0f, 1f, 0f, 1.0f), dhgrDither, 100.0);
    }
    @Test
    public void blueHGRTest() {
        testSolidColor(new Color(0f, 0f, 1f, 1.0f), hgrDither, 162.0);
    }
    @Test
    public void blueDHGRTest() {
        testSolidColor(new Color(0f, 0f, 1f, 1.0f), dhgrDither, 100.0);
    }

    
    private void testSolidColor(Color color, ImageDitherEngine engine, Double maxDelta) {
        WritableImage source = new WritableImage(560, 192);
        fillColor(source, color);
        WritableImage converted = getTestConversion(engine, source);
        assertLowError(source, converted, 16, maxDelta);
    }

    public void assertExactImage(Image img1, Image img2) throws AssertionError {
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                int col1 = img1.getPixelReader().getArgb(x, y) & 0x0FFFFFF;
                int col2 = img2.getPixelReader().getArgb(x, y) & 0x0FFFFFF;
                if (col1 != col2) {
                    throw new AssertionError("Pixels are not the same color at " + x + "," + y + "; (" + Integer.toHexString(col1) + " vs " + Integer.toHexString(col2));
                }
            }
        }
    }

    public void assertLowError(Image img1, Image img2, int gridSize, double maxError) throws AssertionError {
        double error = getAverageImageError(img1, img2, gridSize);
        if (error > maxError) {
            throw new AssertionError("Average error is greater than threshold: " + error + " (max is " + maxError + ")");
        }
    }

    public void assertHighError(Image img1, Image img2, int gridSize, double minError) throws AssertionError {
        double error = getAverageImageError(img1, img2, gridSize);
        if (error < minError) {
            throw new AssertionError("Average error is lower than threshold: " + error + " (min is " + minError + ")");
        }
    }

    // Evaluates a pair of images as a grid of square regions
    // Returns the average error value of all regions
    public double getAverageImageError(Image img1, Image img2, int gridSize) {
        double totalError = 0;
        double regionCount = 0;
        for (int x = 0; x < img1.getWidth(); x += gridSize) {
            int x2 = (int) Math.min(img1.getWidth(), x + gridSize);
            for (int y = 0; y < img1.getHeight(); y += gridSize) {
                int y2 = (int) Math.min(img1.getHeight(), y + gridSize);
                totalError += averageErrorForRegion(img1, img2, x, x2, y, y2);
                regionCount++;
            }
        }
        return totalError / regionCount;
    }

    public double averageErrorForRegion(Image img1, Image img2, int x1, int x2, int y1, int y2) {
        int[] col1 = getAverageColor(img1, x1, x2, y1, y2);
        int[] col2 = getAverageColor(img2, x1, x2, y1, y2);
        return Palette.distance(col1, col2);
    }

    public int[] getAverageColor(Image img, int x1, int x2, int y1, int y2) {
        long[] colors = new long[3];
        long pixelCount = 0;
        for (int x = x1; x < x2 && x < img.getWidth(); x++) {
            for (int y = y1; y < y2 && y < img.getHeight(); y++) {
                int color = img.getPixelReader().getArgb(x, y) & 0x0ffffff;
                int[] col = Palette.parseIntColor(color);
                colors[0] += (long) col[0];
                colors[1] += (long) col[1];
                colors[2] += (long) col[2];
                pixelCount++;
            }
        }
        return new int[]{
            (int) (colors[0] / pixelCount),
            (int) (colors[1] / pixelCount),
            (int) (colors[2] / pixelCount)
        };
    }

    private void configureSierraLite(ImageDitherEngine ditherEngine) {
        int[][] coefficients = new int[][]{
            {0, 0, 0}, {0, 1, 0}, {0, 1, 0}, {2, 0, 0}, {0, 0, 0}};
        ditherEngine.setCoefficients(coefficients);
        ditherEngine.setDivisor(5);
    }

    private void fillColor(WritableImage img, Color color) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                img.getPixelWriter().setColor(x, y, color);
            }
        }
    }

    private WritableImage getTestConversion(ImageDitherEngine dither, WritableImage source) {
        dither.setSourceImage(source);
        configureSierraLite(dither);
        dither.dither(true);
        return dither.getPreviewImage();
    }
}
