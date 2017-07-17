/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.TileSelectModal;
import org.badvision.outlaweditor.ui.ToolType;

/**
 *
 * @author brobert
 */
public class MapEditor extends Editor<Map, MapEditor.DrawMode> implements EventHandler<MouseEvent> {

    Pane anchorPane;
    Canvas drawCanvas;
    private Tile currentTile;
    int posX = 0;
    int posY = 0;
    double zoom = 1.0;
    DrawMode drawMode = DrawMode.Pencil1px;
    TileMap currentMap;
    double tileWidth = getCurrentPlatform().tileRenderer.getWidth() * zoom;
    double tileHeight = getCurrentPlatform().tileRenderer.getHeight() * zoom;
    Color cursorAssistColor = new Color(0.2, 0.2, 1.0, 0.4);

    @Override
    protected void onEntityUpdated() {
        currentMap = new TileMap(getEntity());
    }

    public TileMap getCurrentMap() {
        return currentMap;
    }

    EventHandler<ScrollEvent> scrollHandler = (ScrollEvent t) -> {
        if (t.isShiftDown()) {
            t.consume();
            if (t.getDeltaY() > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        }
    };

    public DrawMode getDrawMode() {
        return drawMode;
    }
    
    @Override
    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
        switch (drawMode) {
            case TileEraser:
                ImageCursor cursor = new ImageCursor(new Image("images/eraser.png"));
                drawCanvas.setCursor(cursor);
                break;
            case Select:
                drawCanvas.setCursor(Cursor.CROSSHAIR);
                break;
            case ScriptPencil:
                drawCanvas.setCursor(Cursor.CLOSED_HAND);
                break;
            case ScriptEraser:
                drawCanvas.setCursor(Cursor.OPEN_HAND);
                break;
            default:
                setCurrentTile(getCurrentTile());
                break;
        }
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void buildEditorUI(Pane tileEditorAnchorPane) {
        anchorPane = tileEditorAnchorPane;
        ApplicationState.getInstance().getPrimaryStage().getScene().addEventHandler(KeyEvent.KEY_PRESSED, this::keyPressed);
        initCanvas();
        redraw();
    }

    private void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.SPACE) {
            e.consume();
            String category = null;
            if (currentTile != null) {
                category = currentTile.getCategory();
            }
            if (this.equals(ApplicationState.getInstance().getController().getVisibleEditor())) {
                TileSelectModal.showTileSelectModal(anchorPane, category, this::setCurrentTile);
            }
        }
    }

    public void initCanvas() {
        if (drawCanvas != null) {
            anchorPane.getChildren().remove(drawCanvas);
        }
        drawCanvas = new Canvas();
        drawCanvas.heightProperty().bind(ApplicationState.getInstance().getPrimaryStage().heightProperty().subtract(120));
        drawCanvas.widthProperty().bind(ApplicationState.getInstance().getPrimaryStage().widthProperty().subtract(200));
//        drawCanvas.widthProperty().bind(anchorPane.widthProperty());
        drawCanvas.widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            redraw();
        });
        drawCanvas.heightProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
            redraw();
        });
        drawCanvas.addEventFilter(ScrollEvent.ANY, scrollHandler);
        drawCanvas.setOnMousePressed(this);
        drawCanvas.setOnMouseMoved(this);
        drawCanvas.setOnMouseDragged(this);
        drawCanvas.setOnMouseDragReleased(this);
        drawCanvas.setOnMouseReleased(this);
        anchorPane.getChildren().add(0, drawCanvas);
        cursorAssistant = new Rectangle(tileWidth, tileHeight, cursorAssistColor);
        cursorAssistant.setMouseTransparent(true);
        cursorAssistant.setEffect(new Glow(1.0));
        anchorPane.getChildren().add(cursorAssistant);
    }

    @Override
    public void addScript(Script script) {
        if (getCurrentMap().getBackingMap().getScripts() == null) {
            getCurrentMap().getBackingMap().setScripts(new Scripts());
        }
        getCurrentMap().getBackingMap().getScripts().getScript().add(script);
    }

    Script selectedScript = null;
    
    public void setSelectedScript(Script script) {
        selectedScript = script;
    }    
    
    public Script getSelectedScript() {
        return selectedScript;
    }
    
    private void drawScript(double x, double y, Script script) {
        if (script != null) {
            getCurrentMap().putLocationScript((int) x, (int) y, script);
        } else {
            getCurrentMap().removeLocationScripts((int) x, (int) y);
        }
        redraw();
    }
    
    public void assignScript(Script script, double x, double y) {
        int xx = (int) (x / tileWidth) + posX;
        int yy = (int) (y / tileHeight) + posY;
        getCurrentMap().putLocationScript(xx, yy, script);
        redraw();
    }

    public void unassignScripts(double x, double y) {
        int xx = (int) (x / tileWidth) + posX;
        int yy = (int) (y / tileHeight) + posY;
        getCurrentMap().removeLocationScripts(xx, yy);
        redraw();
    }

    public void togglePanZoom() {
        anchorPane.getChildren().stream().filter((n) -> !(n == drawCanvas)).forEach((n) -> {
            n.setVisible(!n.isVisible());
        });
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
        zoom += delta;
        zoom = Math.min(Math.max(0.15, zoom), 4.0);
        tileWidth = getCurrentPlatform().tileRenderer.getWidth() * zoom;
        tileHeight = getCurrentPlatform().tileRenderer.getHeight() * zoom;
        fullRedraw.set(true);
        redraw();
    }

    @Override
    public void observedObjectChanged(Map object) {
        redraw();
    }
    private AtomicBoolean fullRedraw = new AtomicBoolean(true);
    private long redrawRequested;
    private Thread redrawThread;

    @Override
    public void redraw() {
        redrawRequested = System.nanoTime();
        if (redrawThread == null || !redrawThread.isAlive()) {
            redrawThread = new Thread(() -> {
                long test = 0;
                while (test != redrawRequested) {
                    test = redrawRequested;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MapEditor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Platform.runLater(() -> {
                    doRedraw(fullRedraw.getAndSet(false));
                });
                redrawThread = null;
            });
            redrawThread.start();
        }
    }

    private synchronized void doRedraw(boolean fullRedraw) {
        prepareCanvas(fullRedraw);
        int cols = (int) (drawCanvas.getWidth() / tileWidth);
        int rows = (int) (drawCanvas.getHeight() / tileHeight);
        for (int x = 0; x <= cols; x++) {
            for (int y = 0; y <= rows; y++) {
                Tile tile = currentMap.get(posX + x, posY + y);
                String id = (tile == null) ? null : tile.getId();
                boolean notSimilar = (id == null) != (tiles[x][y] == null);
                notSimilar |= (id != null && !id.equals(tiles[x][y]));
                if (notSimilar) {
                    tiles[x][y] = id;
                    doDraw(x, y, tile);
                }
            }
        }
        for (int x = 0; x <= cols; x++) {
            for (int y = 0; y <= rows; y++) {
                if (highlightScripts(x, y, currentMap.getLocationScripts(posX + x, posY + y))) {
                    tiles[x][y] = "SCRIPT";
                }
            }
        }
        // Reposition arrows
        anchorPane.getChildren().get(1).setLayoutX((drawCanvas.getWidth() - 30) / 2);
        anchorPane.getChildren().get(2).setLayoutY((drawCanvas.getHeight() - 30) / 2);
        anchorPane.getChildren().get(3).setLayoutX((drawCanvas.getWidth() - 30) / 2);
        anchorPane.getChildren().get(4).setLayoutY((drawCanvas.getHeight() - 30) / 2);
    }

    String[][] tiles = null;

    private void prepareCanvas(boolean forceReset) {
        int cols = (int) (drawCanvas.getWidth() / tileWidth);
        int rows = (int) (drawCanvas.getHeight() / tileHeight);

        boolean reset = tiles == null || tiles.length <= cols || tiles[0] == null || tiles[0].length <= rows;
        if (forceReset || reset) {
            tiles = new String[cols + 1][rows + 1];
            fillEmpty(0, 0, drawCanvas.getWidth(), drawCanvas.getHeight());
        }
    }

    private Paint getFillPattern(double x, double y) {
        boolean oddEven = (x % 20) < 10;
        oddEven ^= (y % 20) < 10;
        return oddEven ? Color.BLACK : Color.NAVY;
    }

    private void doDraw(int x, int y, Tile tile) {
        double xx = x * tileWidth;
        double yy = y * tileHeight;
        if (tile != null) {
            drawCanvas.getGraphicsContext2D().drawImage(TileUtils.getImage(tile, getCurrentPlatform()), xx, yy, tileWidth, tileHeight);
        } else {
            fillEmpty(xx, yy, xx + tileWidth, yy + tileHeight);
        }
    }

    int patternSize = 10;

    private void fillEmpty(double startX, double startY, double endX, double endY) {
        for (double x = startX; x < endX; x = x - (x % patternSize) + patternSize) {
            for (double y = startY; y < endY; y = y - (y % patternSize) + patternSize) {
                double width = Math.min(patternSize, endX - x);
                double height = Math.min(patternSize, endY - y);
                drawCanvas.getGraphicsContext2D().setFill(getFillPattern(x, y));
                drawCanvas.getGraphicsContext2D().fillRect(x, y, width, height);
            }
        }
    }

    private static final int DASH_LENGTH = 3;

    private final Set<Script> invisibleScripts = new HashSet<>();

    public boolean isScriptVisible(Script script) {
        return !invisibleScripts.contains(script);
    }

    public void setScriptVisible(Script script, boolean visible) {
        setScriptVisible(script, visible, true);
    }

    public void setScriptVisible(Script script, boolean visible, boolean redraw) {
        if (visible) {
            invisibleScripts.remove(script);
        } else {
            invisibleScripts.add(script);
        }
        if (redraw) {
            redraw();
        }
    }

    private boolean highlightScripts(int x, int y, List<Script> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return false;
        }
        List<Script> visibleScripts = scripts.stream().filter(this::isScriptVisible).collect(Collectors.toList());
        if (visibleScripts.isEmpty()) {
            return false;
        }
        GraphicsContext gc = drawCanvas.getGraphicsContext2D();
        int idx = 0;
        double xx = x * tileWidth;
        double yy = y * tileHeight;
        gc.setLineWidth(4);
        for (int i = 0; i < tileWidth - 2; i += DASH_LENGTH) {
            idx = (idx + 1) % visibleScripts.size();
            gc.beginPath();
            gc.moveTo(xx, yy);
            currentMap.getScriptColor(visibleScripts.get(idx)).ifPresent(gc::setStroke);
            xx += DASH_LENGTH;
            gc.lineTo(xx, yy);
            gc.setEffect(new DropShadow(2, Color.BLACK));
            gc.stroke();
        }
        for (int i = 0; i < tileHeight - 2; i += DASH_LENGTH) {
            idx = (idx + 1) % visibleScripts.size();
            gc.beginPath();
            gc.moveTo(xx, yy);
            currentMap.getScriptColor(visibleScripts.get(idx)).ifPresent(gc::setStroke);
            yy += DASH_LENGTH;
            gc.lineTo(xx, yy);
            gc.setEffect(new DropShadow(2, Color.BLACK));
            gc.stroke();
        }
        for (int i = 0; i < tileWidth - 2; i += DASH_LENGTH) {
            idx = (idx + 1) % visibleScripts.size();
            gc.beginPath();
            gc.moveTo(xx, yy);
            currentMap.getScriptColor(visibleScripts.get(idx)).ifPresent(gc::setStroke);
            xx -= DASH_LENGTH;
            gc.lineTo(xx, yy);
            gc.setEffect(new DropShadow(2, Color.BLACK));
            gc.stroke();
        }
        for (int i = 0; i < tileHeight - 2; i += DASH_LENGTH) {
            idx = (idx + 1) % visibleScripts.size();
            gc.beginPath();
            gc.moveTo(xx, yy);
            currentMap.getScriptColor(visibleScripts.get(idx)).ifPresent(gc::setStroke);
            yy -= DASH_LENGTH;
            gc.lineTo(xx, yy);
            gc.setEffect(new DropShadow(2, Color.BLACK));
            gc.stroke();
        }
        return true;
    }

    public void setupDragDrop(TransferHelper<Script> scriptHelper, TransferHelper<ToolType> toolHelper) {
        scriptHelper.registerDropSupport(drawCanvas, (Script script, double x, double y) -> {
            assignScript(script, x, y);
        });
        toolHelper.registerDropSupport(drawCanvas, (ToolType tool, double x, double y) -> {
            unassignScripts(x, y);
        });

    }

    @Override
    public void unregister() {
        drawCanvas.widthProperty().unbind();
        drawCanvas.heightProperty().unbind();
        anchorPane.getChildren().remove(drawCanvas);
        currentMap.updateBackingMap();
        ApplicationState.getInstance().getPrimaryStage().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, this::keyPressed);
    }

    /**
     * @return the currentTile
     */
    public Tile getCurrentTile() {
        return currentTile;
    }

    /**
     * @param currentTile the currentTile to set
     * @return Same tile (necessary for callback support)
     */
    public Tile setCurrentTile(Tile currentTile) {
        if (drawMode == DrawMode.TileEraser) {
            drawMode = DrawMode.Pencil1px;
        }
        this.currentTile = currentTile;
        ImageCursor cursor = new ImageCursor(TileUtils.getImage(currentTile, getCurrentPlatform()), 2, 2);
        drawCanvas.setCursor(cursor);
        return currentTile;
    }

    public void showPreview() {
        byte[] data = getCurrentPlatform().imageRenderer.renderPreview(currentMap, posX, posY, getCurrentPlatform().maxImageWidth, getCurrentPlatform().maxImageHeight);
        WritableImage img = getCurrentPlatform().imageRenderer.renderImage(null, data, getCurrentPlatform().maxImageWidth, getCurrentPlatform().maxImageHeight);
        Stage stage = new Stage();
        stage.setTitle("Preview");
        ImageView imgView = new ImageView(img);
        Group root = new Group(imgView);
        stage.setScene(new Scene(root, img.getWidth(), img.getHeight()));
        stage.show();
    }

    @Override
    public void copy() {
        byte[] data = getCurrentPlatform().imageRenderer.renderPreview(currentMap, posX, posY, getCurrentPlatform().maxImageWidth, getCurrentPlatform().maxImageHeight);
        WritableImage img = getCurrentPlatform().imageRenderer.renderImage(null, data, getCurrentPlatform().maxImageWidth, getCurrentPlatform().maxImageHeight);
        java.util.Map<DataFormat, Object> clip = new HashMap<>();
        clip.put(DataFormat.IMAGE, img);
        if (drawMode != DrawMode.Select || startX >= 0) {
            clip.put(DataFormat.PLAIN_TEXT, "selection/map/" + ApplicationState.getInstance().getGameData().getMap().indexOf(getEntity()) + "/" + getSelectionInfo());
        }
        Clipboard.getSystemClipboard().setContent(clip);
        clearSelection();
    }

    @Override
    public String getSelectedAllInfo() {
        setSelectionArea(posX, posY, posX + 19, posY + 11);
        String result = getSelectionInfo();
        return result;
    }

    @Override
    public void paste() {
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.PLAIN_TEXT)) {
            String clipboardInfo = (String) Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT);
            java.util.Map<String, Integer> selection = TransferHelper.getSelectionDetails(clipboardInfo);
            if (selection.containsKey("map")) {
                trackState();
                Map sourceMap = ApplicationState.getInstance().getGameData().getMap().get(selection.get("map"));
                TileMap source = getCurrentMap();
                if (!sourceMap.equals(getCurrentMap().getBackingMap())) {
                    source = new TileMap(sourceMap);
                } else {
                    source.updateBackingMap();
                }
                int height = selection.get("y2") - selection.get("y1");
                int width = selection.get("x2") - selection.get("x1");
                int x1 = selection.get("x1");
                int y1 = selection.get("y1");
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        plot(x + lastX, y + lastY, source.get(x + x1, y + y1));
                    }
                }
            }
        }
    }

    @Override
    public void select() {
        setDrawMode(DrawMode.Select);
    }

    @Override
    public void selectNone() {
        clearSelection();
    }

    Rectangle cursorAssistant;

    private void updateCursorAssistant(MouseEvent t) {
        if (t.getEventType() == MouseEvent.MOUSE_EXITED) {
            cursorAssistant.setVisible(false);
        } else {
            cursorAssistant.setVisible(true);
            cursorAssistant.setWidth(tileWidth);
            cursorAssistant.setHeight(tileHeight);
            cursorAssistant.setTranslateX(t.getX() - (t.getX() % tileWidth));
            cursorAssistant.setTranslateY(t.getY() - (t.getY() % tileHeight));
        }
    }

    public void removeScript(Script script) {
        getCurrentMap().removeScriptFromMap(script);
        redraw();
    }

    private org.badvision.outlaweditor.api.Platform getCurrentPlatform() {
        return ApplicationState.getInstance().getCurrentPlatform();
    }

    @Override
    public void copyData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean hasScriptsAt(int x, int y) {
        List<Script> scripts = currentMap.getLocationScripts(x, y);
        return scripts != null && !scripts.isEmpty();
    }

    public static enum DrawMode {

        Pencil1px, Pencil3px, Pencil5px, FilledRect, TileEraser(false), ScriptPencil(false), Select(false), ScriptEraser(false);

        boolean requireTile = false;

        DrawMode() {
            this(true);
        }

        DrawMode(boolean require) {
            requireTile = require;
        }

        public boolean requiresCurrentTile() {
            return requireTile;
        }
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

    StringProperty cursorInfo = new SimpleStringProperty();

    public StringProperty cursorInfoProperty() {
        return cursorInfo;
    }

    public static Rectangle selectRect = null;
    public double selectScreenStartX = 0;
    public double selectScreenStartY = 0;

    private void startSelection(double x, double y) {
        selectRect = new Rectangle(1, 1, Color.NAVY);
        selectRect.setTranslateX(x);
        selectRect.setTranslateY(y);
        selectRect.setOpacity(0.5);
        selectScreenStartX = x;
        selectScreenStartY = y;
        anchorPane.getChildren().add(selectRect);
    }

    private void updateSelection(double x, double y) {
        if (selectRect == null || selectScreenStartX < 0 || selectScreenStartY < 0) {
            clearSelection();
            startSelection(x, y);
        }

        double minX = Math.min(selectScreenStartX, x);
        double minY = Math.min(selectScreenStartY, y);
        double maxX = Math.max(selectScreenStartX, x);
        double maxY = Math.max(selectScreenStartY, y);
        selectRect.setTranslateX(minX);
        selectRect.setTranslateY(minY);
        selectRect.setWidth(maxX - minX);
        selectRect.setHeight(maxY - minY);
        setSelectionArea(
                (int) (minX / tileWidth + posX),
                (int) (minY / tileHeight + posY),
                (int) (maxX / tileWidth + posX),
                (int) (maxY / tileHeight + posY)
        );
    }

    private void fillSelection() {
        for (int yy = startY; yy <= endY; yy++) {
            for (int xx = startX; xx <= endX; xx++) {
                plot(xx, yy, getCurrentTile());
            }
        }
        clearSelection();
    }

    private void clearSelection() {
        anchorPane.getChildren().remove(selectRect);
        selectRect = null;
        setSelectionArea(-1, -1, -1, -1);
    }

    public static int lastX = -1;
    public static int lastY = -1;
    DrawMode lastDrawMode = null;
    Tile lastTile = null;

    @Override
    protected void trackState() {
        currentMap.updateBackingMap();
        super.trackState();
    }

    @Override
    public void handle(MouseEvent t) {
        int x = (int) (t.getX() / tileWidth) + posX;
        int y = (int) (t.getY() / tileHeight) + posY;
        updateCursorAssistant(t);
        cursorInfo.set("X=" + x + " Y=" + y);
        if (!t.isPrimaryButtonDown() && t.getEventType() != MouseEvent.MOUSE_RELEASED && drawMode != DrawMode.FilledRect) {
            return;
        }
        if (getCurrentTile() == null && drawMode.requiresCurrentTile()) {
            return;
        }
        t.consume();
        boolean canSkip = false;
        if (getCurrentTile() == lastTile && x == lastX && y == lastY && drawMode == lastDrawMode) {
            canSkip = true;
        }
        lastX = x;
        lastY = y;
        lastDrawMode = drawMode;
        lastTile = getCurrentTile();
        switch (drawMode) {
            case TileEraser: {
                if (canSkip) {
                    return;
                }
                trackState();
                plot(x, y, null);
                redraw();
                break;
            }
            case Pencil1px:
                if (canSkip) {
                    return;
                }
                trackState();
                plot(x, y, getCurrentTile());
                break;
            case Pencil3px:
                if (canSkip) {
                    return;
                }
                trackState();
                drawBrush(x, y, 2, getCurrentTile());
                break;
            case Pencil5px:
                if (canSkip) {
                    return;
                }
                trackState();
                drawBrush(x, y, 5, getCurrentTile());
                break;
            case FilledRect:
                updateSelection(t.getX(), t.getY());
                if (t.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                    trackState();
                    fillSelection();
                }
            case ScriptPencil:
                if (canSkip) {
                    return;
                }
                trackState();
                drawScript(x, y, getSelectedScript());
                break;
            case ScriptEraser:
                if (canSkip || !hasScriptsAt(x, y)) {
                    return;
                }
                trackState();
                drawScript(x, y, null);
                break;
            case Select:
                updateSelection(t.getX(), t.getY());
                if (t.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                    selectScreenStartX = selectScreenStartY = -1;
                }

        }
    }
}
