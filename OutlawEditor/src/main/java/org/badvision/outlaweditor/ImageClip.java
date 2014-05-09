package org.badvision.outlaweditor;

import javafx.scene.shape.Rectangle;
import org.badvision.outlaweditor.data.xml.Image;

/**
 * Details about part of an image
 * @author blurry
 * @param <T> Represents the image renderer that can produce this image
 */
public class ImageClip<T extends ImageRenderer> {
    private final int clipId;
    private Rectangle bounds;
    private Image source;
    private ImageRenderer renderer;
    private Platform platform;
    private boolean allSelected;
    public ImageClip(Image src, boolean all, ImageRenderer r, Platform p) {
        source = src;
        renderer = r;
        platform = p;
        clipId = (int) (Math.random() * (double) Integer.MAX_VALUE);        
    }
    
    public ImageClip(Image src, int x1, int y1, int x2, int y2, ImageRenderer r, Platform p) {
        this(src, false, r, p);
        bounds = new Rectangle(
                Math.min(x1,x2), 
                Math.min(y1,y2),
                Math.abs(x2-x1),
                Math.abs(y2-y1));
    }
    
    /**
     * @return the clipId
     */
    public int getClipId() {
        return clipId;
    }

    /**
     * @return the bounds
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * @param bounds the bounds to set
     */
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * @return the source
     */
    public Image getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(Image source) {
        this.source = source;
    }

    /**
     * @return the renderer
     */
    public ImageRenderer getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(ImageRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return the platform
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * @param platform the platform to set
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
