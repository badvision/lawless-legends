package org.badvision.outlaweditor.apple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
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
import org.badvision.outlaweditor.FileUtils;
import org.badvision.outlaweditor.ImageEditor;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.ui.UIAction;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;

/**
 *
 * @author brobert
 */
public class AppleImageEditor extends ImageEditor implements EventHandler<MouseEvent> {

    public int[] currentFillPattern = FillPattern.White_PC.bytePattern;
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
        screen.setOnMousePressed(this);
        screen.setOnMouseClicked(this);
        screen.setOnMouseReleased(this);
        screen.setOnMouseDragged(this);
        screen.setOnMouseDragReleased(this);
    }

    @Override
    public void buildPatternSelector(Menu tilePatternMenu) {
        FillPattern.buildMenu(tilePatternMenu, (FillPattern object) -> {
            changeCurrentPattern(object);
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
        currentImage = getPlatform().imageRenderer.renderScanline(currentImage, y, getWidth(), getImageData());
    }

    @Override
    public void redraw() {
        System.out.println("Redraw " + getPlatform().name());
        currentImage = getPlatform().imageRenderer.renderImage(currentImage, getImageData(), getWidth(), getHeight());
        anchorPane.getChildren().get(1).setLayoutX((anchorPane.getWidth() - 30) / 2);
        anchorPane.getChildren().get(2).setLayoutY((anchorPane.getHeight() - 30) / 2);
        anchorPane.getChildren().get(3).setLayoutX((anchorPane.getWidth() - 30) / 2);
        anchorPane.getChildren().get(4).setLayoutY((anchorPane.getHeight() - 30) / 2);
    }
    private byte[] imageData = null;

    public byte[] getImageData() {
        if (imageData == null) {
            PlatformData data = getPlatformData(getPlatform());
            if (data == null) {
                data = new PlatformData();
                data.setWidth(getPlatform().maxImageWidth);
                data.setHeight(getPlatform().maxImageHeight);
                data.setPlatform(getPlatform().name());
                data.setValue(getPlatform().imageRenderer.createImageBuffer(getPlatform().maxImageWidth, getPlatform().maxImageHeight));
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

    public void setDataAndRedraw(byte[] data) {
        setData(data);
        redraw();
    }
    
    @Override
    public void togglePanZoom() {
        anchorPane.getChildren().stream().filter((n) -> !(n == screen)).forEach((n) -> {
            n.setVisible(!n.isVisible());
        });
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
        // Scale the image and move it so the upper-left corner is still in the right place.
        screen.setScaleX(zoom);
        screen.setScaleY(zoom);
        screen.setTranslateX(getWidth()*7 * (zoom-1));
        screen.setTranslateY(getHeight() * (zoom-1));
        redraw();
    }

    @Override
    public void handle(MouseEvent t) {
        if (performAction(t.isShiftDown() || t.isSecondaryButtonDown(), t.getEventType().equals(MouseEvent.MOUSE_RELEASED), (int) t.getX() / xScale, (int) t.getY() / yScale)) {
            t.consume();
        }

    }
    protected int lastActionX = -1;
    protected int lastActionY = -1;
    protected long debounce = -1;
    public static long DEBOUNCE_THRESHOLD = 50;

    public boolean performAction(boolean alt, boolean released, int x, int y) {
        if (debounce != -1) {
            long ellapsed = System.currentTimeMillis() - debounce;
            if (ellapsed <= DEBOUNCE_THRESHOLD) {
                return false;
            }
            debounce = -1;
        }
        y = Math.min(Math.max(y, 0), getHeight() - 1);
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
                    return false;
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
                    return false;
                }
                plot(x, y, currentFillPattern, hiBitMatters); // [ref BigBlue1_30]
                redrawScanline(y);
                break;
            case Pencil3px:
                if (canSkip) {
                    return false;
                }
                drawBrush(x, y, 3, currentFillPattern, hiBitMatters);
                break;
            case Pencil5px:
                if (canSkip) {
                    return false;
                }
                drawBrush(x, y, 5, currentFillPattern, hiBitMatters);
                break;
            case Rectangle:
                if (released) {
                    fillSelection(x, y);
                    redraw();
                    debounce = System.currentTimeMillis();
                } else {
                    updateSelection(x, y);
                }
                break;
        }
        return true;
//        observedObjectChanged(getEntity());
    }
    public static Rectangle selectRect = null;
    public int selectStartX = -1;
    public int selectStartY = -1;

    private void startSelection(int x, int y) {
        selectRect = new Rectangle(1, 1, Color.NAVY);
        selectRect.setTranslateX(x * xScale * zoom);
        selectRect.setTranslateY(y * yScale * zoom);
        selectRect.setOpacity(0.5);
        selectStartX = x;
        selectStartY = y;
        anchorPane.getChildren().add(selectRect);
    }

    private void updateSelection(int x, int y) {
        if (selectRect == null) {
            startSelection(x, y);
        }

        double minX = Math.min(selectStartX, x) * xScale * zoom;
        double minY = Math.min(selectStartY, y) * yScale * zoom;
        double maxX = Math.max(selectStartX, x) * xScale * zoom;
        double maxY = Math.max(selectStartY, y) * yScale * zoom;
        selectRect.setTranslateX(minX);
        selectRect.setTranslateY(minY);
        selectRect.setWidth(maxX - minX);
        selectRect.setHeight(maxY - minY);
    }

    private void fillSelection(int x, int y) {
        if (selectRect == null) {
            return;
        }
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
        int pat = pattern[(y % 16) * 4 + ((x / 7) % 4)];  // [ref BigBlue1_20]
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
        data[y * getWidth() + (x / 7)] |= (1 << (x % 7));  // [ref BigBlue1_10]
        if (!on) {
            data[y * getWidth() + (x / 7)] ^= (1 << (x % 7));
        }
        setData(data);
    }

    public int getWidth() {
        return getPlatformData(getPlatform()).getWidth();
    }

    public int getHeight() {
        return getPlatformData(getPlatform()).getHeight();
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
        }
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.IMAGE)) {
            javafx.scene.image.Image image = Clipboard.getSystemClipboard().getImage();
            
            importImage(image);            
        }
    }
    //selection/map/2/x1/0/y1/0/x2/19/y2/11

    public boolean pasteAppContent(String contentPath) {
        System.out.println("Clipboard >> " + contentPath);
        if (contentPath.startsWith("selection/map")) {
            String[] bufferDetails = contentPath.substring(14).split("/");
            int mapNumber = Integer.parseInt(bufferDetails[0]);
            Map<String, Integer> details = new HashMap<>();
            for (int i = 1; i < bufferDetails.length; i += 2) {
                details.put(bufferDetails[i], Integer.parseInt(bufferDetails[i + 1]));
            }
            TileMap map = new TileMap(Application.gameData.getMap().get(mapNumber));
            byte[] buf = getPlatform().imageRenderer.renderPreview(
                    map,
                    details.get("x1"),
                    details.get("y1"),
                    getWidth(),
                    getHeight());
            setData(buf);
            redraw();
            return true;
        }
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
        ImageDitherEngine ditherEngine = new ImageDitherEngine(getPlatform());
        ditherEngine.setTargetCoordinates(0,0);
        UIAction.openImageConversionModal(image, ditherEngine, getWidth(), getHeight(), this::setDataAndRedraw);
    }

    private int calculateHiresOffset(int y) {
        return calculateTextOffset(y >> 3) + ((y & 7) << 10);
    }

    private int calculateTextOffset(int y) {
        return ((y & 7) << 7) + 40 * (y >> 3);
    }

    @Override
    public void exportImage() {
        byte[] output = new byte[0x02000];
        int counter = 0;
        for (int y = 0; y < 192; y++) {
            int offset = calculateHiresOffset(y);
            for (int x = 0; x < 40; x++) {
                output[offset + x] = getImageData()[counter++];
            }
        }
        File out = FileUtils.getFile(null, "Export image", true, FileUtils.Extension.BINARY, FileUtils.Extension.ALL);
        if (out == null) {
            return;
        }
        try {
            FileOutputStream outStream = new FileOutputStream(out);
            outStream.write(output);
            outStream.flush();
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(AppleImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {
        UIAction.confirm("Do you want to scale the image?  If you select no, the image will be cropped as needed.", () -> {
            rescale(newWidth, newHeight);
        }, () -> {
            crop(newWidth, newHeight);
        });
    }

    /**
     * This takes the current image and dithers it to match the new image
     * dimensions Most likely it will result in a really bad looking resized
     * copy but in some cases might look okay
     *
     * @param newWidth
     * @param newHeight
     */
    public void rescale(int newWidth, int newHeight) {
    }

    /**
     * Crops the image (if necessary) or resizes the image leaving the extra
     * space blank (black)
     *
     * @param newWidth
     * @param newHeight
     */
    public void crop(int newWidth, int newHeight) {
    }
}
