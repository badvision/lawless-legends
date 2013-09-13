/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor;

import javafx.scene.control.Menu;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;

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

    public abstract void resize(int newWidth, int newHeight);
    
    public PlatformData getPlatformData(Platform p) {        
        for (PlatformData data : getEntity().getDisplayData()) {
            if (data.getPlatform().equalsIgnoreCase(p.name())) {
                return data;
            }
        }
        return null;
    }
}
