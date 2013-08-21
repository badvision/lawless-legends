/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor;

import javafx.scene.control.Menu;
import org.badvision.outlaweditor.data.xml.Image;

/**
 *
 * @author brobert
 */
public abstract class ImageEditor extends Editor<Image, ImageEditor.DrawMode> {
    public static enum DrawMode {
        Toggle, Pencil1px, Pencil3px, Pencil5px, Rectangle, Circle, Stamp
    }

    abstract public void buildPatternSelector(Menu tilePatternMenu);

    abstract public void redraw();

    public abstract void scrollBy(int deltaX, int deltaY);

    public abstract void togglePanZoom();

    public abstract void zoomIn();

    public abstract void zoomOut();
    
    public abstract void exportImage();
}
