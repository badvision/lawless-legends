package org.badvision.outlaweditor;

import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.data.TileMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * @author brobert
 */
public abstract class ImageRenderer {

    public abstract WritableImage renderImage(WritableImage img, byte[] rawImage, int width, int height);

    public abstract byte[] createImageBuffer(int width, int height);

    public abstract byte[] renderPreview(TileMap map, int startX, int startY, int width, int height);

    public abstract WritableImage renderScanline(WritableImage currentImage, int y, int width, byte[] imageData);

}
