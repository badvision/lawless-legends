/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.apple;

import java.util.HashMap;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.ImageEditor;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;

/**
 *
 * @author brobert
 */
public class AppleImageEditor extends ImageEditor implements EventHandler<MouseEvent> {

    public int[] currentFillPattern = FillPattern.White.bytePattern;
    public boolean hiBitMatters = true;
    protected DrawMode currentDrawMode = DrawMode.Pencil1px;
    protected WritableImage currentImage;
    protected AnchorPane anchorPane;
    protected ImageView screen;
    protected int posX = 0;
    protected int posY = 0;
    protected double zoom = 1.0;
    protected int xScale = 2;
    protected int yScale = 2;
    
    public Platform getPlatform() {
        return Platform.AppleII;
    }

    @Override
    public void buildEditorUI(AnchorPane editorAnchorPane) {
        anchorPane = editorAnchorPane;
        redraw();
        screen = new ImageView(currentImage);
        anchorPane.getChildren().add(0, screen);
        screen.setOnMouseClicked(this);
        screen.setOnMouseReleased(this);
        screen.setOnMouseDragged(this);
        screen.setOnMouseDragReleased(this);
    }

    @Override
    public void buildPatternSelector(Menu tilePatternMenu) {
        FillPattern.buildMenu(tilePatternMenu, new DataObserver<FillPattern>() {
            @Override
            public void observedObjectChanged(FillPattern object) {
                changeCurrentPattern(object);
            }
        });
    }

    public void changeCurrentPattern(FillPattern pattern) {
        currentFillPattern = pattern.getBytePattern();
        hiBitMatters = pattern.hiBitMatters;
        lastActionX = -1;
        lastActionY = -1;
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        currentDrawMode = drawMode;
        lastActionX = -1;
        lastActionY = -1;
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregister() {
        anchorPane.getChildren().remove(screen);
    }

    @Override
    public void observedObjectChanged(Image object) {
        redraw();
    }

    public void redrawScanline(int y) {
        currentImage = getPlatform().imageRenderer.renderScanline(currentImage, y, getImageData());
    }

    @Override
    public void redraw() {
        System.out.println("Redraw "+getPlatform().name());
        currentImage = getPlatform().imageRenderer.renderImage(currentImage, getImageData());
        anchorPane.getChildren().get(1).setLayoutX((anchorPane.getWidth() - 30) / 2);
        anchorPane.getChildren().get(2).setLayoutY((anchorPane.getHeight() - 30) / 2);
        anchorPane.getChildren().get(3).setLayoutX((anchorPane.getWidth() - 30) / 2);
        anchorPane.getChildren().get(4).setLayoutY((anchorPane.getHeight() - 30) / 2);
    }
    private byte[] imageData = null;

    public byte[] getImageData() {
        if (imageData == null) {
            PlatformData data = null;
            for (PlatformData d : getEntity().getDisplayData()) {
                if (d.getPlatform().equalsIgnoreCase(getPlatform().name())) {
                    data = d;
                    break;
                }
            }
            if (data == null) {
                data = new PlatformData();
                data.setWidth(40);
                data.setHeight(192);
                data.setPlatform(getPlatform().name());
                data.setValue(getPlatform().imageRenderer.createImageBuffer());
                getEntity().getDisplayData().add(data);
            }
            imageData = data.getValue();
        }
        return imageData;
    }

    public void setData(byte[] data) {
        imageData = data;
        for (PlatformData d : getEntity().getDisplayData()) {
            if (d.getPlatform().equalsIgnoreCase(getPlatform().name())) {
                d.setValue(data);
                break;
            }
        }
    }

    @Override
    public void togglePanZoom() {
        for (Node n : anchorPane.getChildren()) {
            if (n == screen) {
                continue;
            }
            n.setVisible(!n.isVisible());
        }
    }

    @Override
    public void scrollBy(int deltaX, int deltaY) {
        posX += deltaX * 10;
        posY += deltaY * 10;
        posX = Math.max(0, posX);
        posY = Math.max(0, posY);
        redraw();
    }

    @Override
    public void zoomOut() {
        if (zoom <= 1) {
            zoom(-0.1);
        } else {
            zoom(-0.25);
        }
    }

    @Override
    public void zoomIn() {
        if (zoom >= 1) {
            zoom(0.25);
        } else {
            zoom(0.1);
        }
    }

    private void zoom(double delta) {
        double oldZoom = zoom;
        zoom += delta;
        zoom = Math.min(Math.max(0.15, zoom), 4.0);
//                    double left = mapEditorScroll.getHvalue();
//                    double top = mapEditorScroll.getVvalue();
//
//                    double pointerX = t.getX();
//                    double pointerY = t.getY();
//
        double ratio = zoom / oldZoom;
//
//                    double newLeft = (left + pointerX) * ratio - pointerX;
//                    double newTop = (top + pointerY) * ratio - pointerY;
        redraw();
    }

    @Override
    public void handle(MouseEvent t) {
        performAction(t.isShiftDown() || t.isSecondaryButtonDown(), t.getEventType().equals(MouseEvent.MOUSE_RELEASED), (int) t.getX() / xScale, (int) t.getY() / yScale);
    }
    protected int lastActionX = -1;
    protected int lastActionY = -1;

    public void performAction(boolean alt, boolean released, int x, int y) {
        y = Math.min(Math.max(y, 0), 191);
        x = Math.min(Math.max(x, 0), (getWidth() * 7) - 1);
        boolean canSkip = false;
        if (lastActionX == x && lastActionY == y) {
            canSkip = true;
        }
        lastActionX = x;
        lastActionY = y;
        switch (currentDrawMode) {
            case Toggle:
                if (canSkip) {
                    return;
                }
                if (alt) {
                    toggleHiBit(x, y);
                } else {
                    toggle(x, y);
                }
                redrawScanline(y);
                break;
            case Pencil1px:
                if (canSkip) {
                    return;
                }
                plot(x, y, currentFillPattern, hiBitMatters);
                redrawScanline(y);
                break;
            case Pencil3px:
                if (canSkip) {
                    return;
                }
                drawBrush(x, y, 3, currentFillPattern, hiBitMatters);
                break;
            case Pencil5px:
                if (canSkip) {
                    return;
                }
                drawBrush(x, y, 5, currentFillPattern, hiBitMatters);
                break;
            case Rectangle:
                if (released) {
                    fillSelection(x, y);
                    redraw();
                } else {
                    updateSelection(x, y);
                }
        }
//        observedObjectChanged(getEntity());
    }
    public static Rectangle selectRect = null;
    public int selectStartX = 0;
    public int selectStartY = 0;

    private void startSelection(int x, int y) {
        selectRect = new Rectangle(1, 1, Color.NAVY);
        selectRect.setTranslateX(x);
        selectRect.setTranslateY(y);
        selectRect.setOpacity(0.5);
        selectStartX = x;
        selectStartY = y;
        anchorPane.getChildren().add(selectRect);
    }

    private void updateSelection(int x, int y) {
        if (selectRect == null) {
            startSelection(x, y);
        }

        double minX = Math.min(selectStartX, x) * xScale;
        double minY = Math.min(selectStartY, y) * yScale;
        double maxX = Math.max(selectStartX, x) * xScale;
        double maxY = Math.max(selectStartY, y) * yScale;
        selectRect.setTranslateX(minX);
        selectRect.setTranslateY(minY);
        selectRect.setWidth(maxX - minX);
        selectRect.setHeight(maxY - minY);
    }

    private void fillSelection(int x, int y) {
        anchorPane.getChildren().remove(selectRect);
        selectRect = null;

        for (int yy = Math.min(selectStartY, y); yy <= Math.max(selectStartY, y); yy++) {
            for (int xx = Math.min(selectStartX, x); xx <= Math.max(selectStartX, x); xx++) {
                plot(xx, yy, currentFillPattern, hiBitMatters);
            }
        }
    }

    public void drawBrush(int x, int y, int size, int[] pattern, boolean hiBitMatters) {
        for (int yy = y - size; yy <= y + size; yy++) {
            for (int xx = x - size; xx <= x + size; xx++) {
                if (Math.sqrt((xx - x) * (xx - x) + (yy - y) * (yy - y)) > size) {
                    continue;
                }
                plot(xx, yy, pattern, hiBitMatters);
            }
            redrawScanline(yy);
        }
    }

    public void plot(int x, int y, int[] pattern, boolean hiBitMatters) {
        if (x < 0 || y < 0 || x >= getWidth() * 7 || y >= getHeight()) {
            return;
        }
        int pat = pattern[(y % 16) * 4 + ((x / 7) % 4)];
        set((pat & (1 << (x % 7))) != 0, x, y);
        if (hiBitMatters) {
            setHiBit(pat >= 128, x, y);
        }
    }

    public void toggleHiBit(int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] ^= 128;
        setData(data);
    }

    public void setHiBit(boolean on, int x, int y) {
        byte[] data = getImageData();
        if (on) {
            data[y * getWidth() + (x / 7)] |= 128;
        } else {
            data[y * getWidth() + (x / 7)] &= 127;
        }
        setData(data);
    }

    public void toggle(int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] ^= (1 << (x % 7));
        setData(data);
    }

    public void set(boolean on, int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] |= (1 << (x % 7));
        if (!on) {
            data[y * getWidth() + (x / 7)] ^= (1 << (x % 7));
        }
        setData(data);
    }

    public int getWidth() {
        return 40;
    }
    public int getHeight() {
        return 192;
    }

    @Override
    public void copy() {
        java.util.Map<DataFormat, Object> clip = new HashMap<>();
        clip.put(DataFormat.IMAGE, currentImage);
        clip.put(DataFormat.PLAIN_TEXT, "selection/image/" + Application.gameData.getImage().indexOf(getEntity()) + "/" + getSelectionInfo());
        Clipboard.getSystemClipboard().setContent(clip);
    }

    @Override
    public void paste() {
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.PLAIN_TEXT)) {
            if (pasteAppContent((String) Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT))) {
                return;
            }
        };
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.IMAGE)) {
            javafx.scene.image.Image image = Clipboard.getSystemClipboard().getImage();
            importImage(image);
        }
    }

    public boolean pasteAppContent(String contentPath) {
        return false;
    }

    @Override
    public void select() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectNone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void importImage(javafx.scene.image.Image image) {
        FloydSteinbergDither.floydSteinbergDither(image, getPlatform(), 0, 0, getWidth(), getHeight(), new FloydSteinbergDither.DitherCallback() {
            @Override
            public void ditherCompleted(byte[] data) {
                setData(data);
                redraw();
            }
        });
    }
}