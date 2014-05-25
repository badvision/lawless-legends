/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import static org.badvision.outlaweditor.Application.currentPlatform;
import org.badvision.outlaweditor.TransferHelper.DropEventHandler;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class MapEditor extends Editor<Map, MapEditor.DrawMode> implements EventHandler<MouseEvent> {

    AnchorPane anchorPane;
    Canvas drawCanvas;
    private Tile currentTile;
    int posX = 0;
    int posY = 0;
    double zoom = 1.0;
    DrawMode drawMode = DrawMode.Pencil1px;
    TileMap currentMap;
    double tileWidth = currentPlatform.tileRenderer.getWidth() * zoom;
    double tileHeight = currentPlatform.tileRenderer.getHeight() * zoom;
    public static TransferHelper<Script> scriptDragDrop = new TransferHelper<>(Script.class);

    @Override
    public void setEntity(Map t) {
        super.setEntity(t);
        currentMap = new TileMap(t);
    }
    public TileMap getCurrentMap() {
        return currentMap;
    }
    
    EventHandler<ScrollEvent> scrollHandler = new EventHandler<ScrollEvent>() {
        @Override
        public void handle(ScrollEvent t) {
            if (t.isShiftDown()) {
                t.consume();
                if (t.getDeltaY() > 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        }
    };

    @Override
    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void buildEditorUI(AnchorPane tileEditorAnchorPane) {
        anchorPane = tileEditorAnchorPane;
        initCanvas();
        redraw();
    }

    public void initCanvas() {
        if (drawCanvas != null) {
            anchorPane.getChildren().remove(drawCanvas);
        }
        drawCanvas = new Canvas();
        drawCanvas.heightProperty().bind(Application.getPrimaryStage().heightProperty().subtract(120));
        drawCanvas.widthProperty().bind(Application.getPrimaryStage().widthProperty().subtract(200));
//        drawCanvas.widthProperty().bind(anchorPane.widthProperty());
        drawCanvas.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                redraw();
            }
        });
        drawCanvas.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                redraw();
            }
        });
        drawCanvas.addEventFilter(ScrollEvent.ANY, scrollHandler);
        drawCanvas.setOnMousePressed(this);
        drawCanvas.setOnMouseDragged(this);
        drawCanvas.setOnMouseDragReleased(this);
        drawCanvas.setOnMouseReleased(this);
        scriptDragDrop.registerDropSupport(drawCanvas, new DropEventHandler<Script>() {
            @Override
            public void handle(Script script, double x, double y) {
                assignScript(script, x, y);
            }
        });
        anchorPane.getChildren().add(0, drawCanvas);
    }

    @Override
    public void addScript(Script script) {
        if (getCurrentMap().getBackingMap().getScripts() == null) {
            getCurrentMap().getBackingMap().setScripts(new Map.Scripts());
        }
        getCurrentMap().getBackingMap().getScripts().getScript().add(script);
    }
    
    public void assignScript(Script script, double x, double y) {
        System.out.println("Dropped " + script.getName() + " at " + x + "," + y);
    }

    public void togglePanZoom() {
        for (Node n : anchorPane.getChildren()) {
            if (n == drawCanvas) {
                continue;
            }
            n.setVisible(!n.isVisible());
        }
    }

    public void scrollBy(int deltaX, int deltaY) {
        posX += deltaX * (drawCanvas.getWidth() / tileWidth / 2);
        posY += deltaY * (drawCanvas.getHeight() / tileWidth / 2);
        posX = Math.max(0, posX);
        posY = Math.max(0, posY);
        redraw();
    }

    public void zoomOut() {
        if (zoom <= 1) {
            zoom(-0.1);
        } else {
            zoom(-0.25);
        }
    }

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
        tileWidth = currentPlatform.tileRenderer.getWidth() * zoom;
        tileHeight = currentPlatform.tileRenderer.getHeight() * zoom;
        redraw();
    }

    @Override
    public void observedObjectChanged(Map object) {
        redraw();
    }
    private long redrawRequested;
    private Thread redrawThread;

    public void redraw() {
        redrawRequested = System.nanoTime();
        if (redrawThread == null || redrawThread.isAlive()) {
            redrawThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    long test = redrawRequested;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MapEditor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    while (test != redrawRequested) {
                        test = redrawRequested;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MapEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            doRedraw();
                        }
                    });
                    redrawThread = null;
                }
            });
            redrawThread.start();
        }
    }

    private synchronized void doRedraw() {
        drawCanvas.getGraphicsContext2D().clearRect(0, 0, drawCanvas.getWidth(), drawCanvas.getHeight());
        int cols = (int) (drawCanvas.getWidth() / tileWidth);
        int rows = (int) (drawCanvas.getHeight() / tileHeight);
        for (int x = 0; x <= cols; x++) {
            for (int y = 0; y <= rows; y++) {
                Tile tile = currentMap.get(posX + x, posY + y);
                doDraw(x, y, tile);
            }
        }
        anchorPane.getChildren().get(1).setLayoutX((drawCanvas.getWidth() - 30) / 2);
        anchorPane.getChildren().get(2).setLayoutY((drawCanvas.getHeight() - 30) / 2);
        anchorPane.getChildren().get(3).setLayoutX((drawCanvas.getWidth() - 30) / 2);
        anchorPane.getChildren().get(4).setLayoutY((drawCanvas.getHeight() - 30) / 2);
    }

    private void doDraw(int x, int y, Tile tile) {
        double xx = x * tileWidth;
        double yy = y * tileHeight;
        if (tile != null) {
            drawCanvas.getGraphicsContext2D().drawImage(TileUtils.getImage(tile, currentPlatform), xx, yy, tileWidth, tileHeight);
        } else {
            drawCanvas.getGraphicsContext2D().clearRect(xx, yy, tileWidth, tileHeight);
        }
    }

    @Override
    public void unregister() {
        drawCanvas.widthProperty().unbind();
        drawCanvas.heightProperty().unbind();
        anchorPane.getChildren().remove(drawCanvas);
        currentMap.updateBackingMap();
    }

    /**
     * @return the currentTile
     */
    public Tile getCurrentTile() {
        return currentTile;
    }

    /**
     * @param currentTile the currentTile to set
     */
    public void setCurrentTile(Tile currentTile) {
        this.currentTile = currentTile;
        ImageCursor cursor = new ImageCursor(TileUtils.getImage(currentTile, currentPlatform), 2, 2);
        drawCanvas.setCursor(cursor);
    }

    public void showPreview() {
        byte[] data = currentPlatform.imageRenderer.renderPreview(currentMap, posX, posY, currentPlatform.maxImageWidth, currentPlatform.maxImageHeight);
        WritableImage img = currentPlatform.imageRenderer.renderImage(null, data, currentPlatform.maxImageWidth, currentPlatform.maxImageHeight);
        Stage stage = new Stage();
        stage.setTitle("Preview");
        ImageView imgView = new ImageView(img);
        Group root = new Group(imgView);
        stage.setScene(new Scene(root, img.getWidth(), img.getHeight()));
        stage.show();
    }

    @Override
    public void copy() {
        byte[] data = currentPlatform.imageRenderer.renderPreview(currentMap, posX, posY, currentPlatform.maxImageWidth, currentPlatform.maxImageHeight);
        WritableImage img = currentPlatform.imageRenderer.renderImage(null, data, currentPlatform.maxImageWidth, currentPlatform.maxImageHeight);
        java.util.Map<DataFormat, Object> clip = new HashMap<>();
        clip.put(DataFormat.IMAGE, img);
        clip.put(DataFormat.PLAIN_TEXT, "selection/map/" + Application.gameData.getMap().indexOf(getEntity()) + "/" + getSelectionInfo());
        Clipboard.getSystemClipboard().setContent(clip);
    }

    @Override
    public String getSelectedAllInfo() {
        setSelectionArea(posX, posY, posX + 19, posY + 11);
        String result = getSelectionInfo();
        setSelectionArea(0, 0, 0, 0);
        return result;
    }

    @Override
    public void paste() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void select() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectNone() {
        setSelectionArea(0, 0, 0, 0);
    }

    public static enum DrawMode {

        Pencil1px, Pencil3px, Pencil5px, FilledRect
    };

    public void plot(final int x, final int y, final Tile t) {
        if (x < 0) {
            return;
        }
        if (y < 0) {
            return;
        }
        if (x >= getEntity().getWidth()) {
            System.out.println("X out of bounds " + getEntity().getWidth());
            return;
        }
        if (y >= getEntity().getHeight()) {
            System.out.println("Y out of bounds " + getEntity().getHeight());
            return;
        }
        currentMap.put(x, y, t);
        doDraw(x - posX, y - posY, t);
    }

    public void drawBrush(int x, int y, int size, Tile t) {
        for (int xx = x - size; xx <= x + size; xx++) {
            for (int yy = y - size; yy <= y + size; yy++) {
                if (Math.sqrt((xx - x) * (xx - x) + (yy - y) * (yy - y)) > size) {
                    continue;
                }
                plot(xx, yy, getCurrentTile());
            }
        }
    }
    public static Rectangle selectRect = null;
    public double selectStartX = 0;
    public double selectStartY = 0;

    private void startSelection(double x, double y) {
        selectRect = new Rectangle(1, 1, Color.NAVY);
        selectRect.setTranslateX(x);
        selectRect.setTranslateY(y);
        selectRect.setOpacity(0.5);
        selectStartX = x;
        selectStartY = y;
        anchorPane.getChildren().add(selectRect);
    }

    private void updateSelection(double x, double y) {
        if (selectRect == null) {
            startSelection(x, y);
        }

        double minX = Math.min(selectStartX, x);
        double minY = Math.min(selectStartY, y);
        double maxX = Math.max(selectStartX, x);
        double maxY = Math.max(selectStartY, y);
        selectRect.setTranslateX(minX);
        selectRect.setTranslateY(minY);
        selectRect.setWidth(maxX - minX);
        selectRect.setHeight(maxY - minY);
    }

    private void fillSelection(double x, double y) {
        anchorPane.getChildren().remove(selectRect);
        selectRect = null;

        int startx = (int) (selectStartX / tileWidth);
        int starty = (int) (selectStartY / tileHeight);
        int endx = (int) (x / tileWidth);
        int endy = (int) (y / tileHeight);
        for (int yy = Math.min(starty, endy); yy <= Math.max(starty, endy); yy++) {
            for (int xx = Math.min(startx, endx); xx <= Math.max(startx, endx); xx++) {
                plot(xx, yy, getCurrentTile());
            }
        }
    }
    public static int lastX = -1;
    public static int lastY = -1;
    DrawMode lastDrawMode = null;
    Tile lastTile = null;

    @Override
    public void handle(MouseEvent t) {
        if (getCurrentTile() == null) {
            System.out.println("No tile selected, ignoring");
            return;
        }
        t.consume();
        int x = (int) (t.getX() / tileWidth) + posX;
        int y = (int) (t.getY() / tileHeight) + posY;
        boolean canSkip = false;
        if (getCurrentTile() == lastTile && x == lastX && y == lastY && drawMode == lastDrawMode) {
            canSkip = true;
        }
        lastX = x;
        lastY = y;
        lastDrawMode = drawMode;
        lastTile = getCurrentTile();
        switch (drawMode) {
            case Pencil1px:
                if (canSkip) {
                    return;
                }
                plot(x, y, getCurrentTile());
                break;
            case Pencil3px:
                if (canSkip) {
                    return;
                }
                drawBrush(x, y, 2, getCurrentTile());
                break;
            case Pencil5px:
                if (canSkip) {
                    return;
                }
                drawBrush(x, y, 5, getCurrentTile());
                break;
            case FilledRect:
                if (t.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                    fillSelection(t.getX(), t.getY());
                } else {
                    updateSelection(t.getX(), t.getY());
                }
        }
    }
}
