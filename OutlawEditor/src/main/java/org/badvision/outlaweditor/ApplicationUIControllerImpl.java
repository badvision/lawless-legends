package org.badvision.outlaweditor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import static org.badvision.outlaweditor.Application.currentPlatform;
import static org.badvision.outlaweditor.Application.gameData;
import static org.badvision.outlaweditor.UIAction.*;
import org.badvision.outlaweditor.apple.AppleTileRenderer;
import static org.badvision.outlaweditor.data.PropertyHelper.*;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 * Actual implementation of Application UI, isolated from auto-generated code
 *
 * @author brobert
 */
public class ApplicationUIControllerImpl extends ApplicationUIController {

    public Tile currentTile = null;
    public TileEditor currentTileEditor = null;
    public org.badvision.outlaweditor.data.xml.Map currentMap = null;
    public MapEditor currentMapEditor = null;
    public Image currentImage = null;
    public ImageEditor currentImageEditor = null;

    @Override
    public void initialize() {
        super.initialize();

        TilesetUtils.addObserver(new org.badvision.outlaweditor.data.DataObserver() {
            @Override
            public void observedObjectChanged(Object object) {
                rebuildTileSelectors();
            }
        });
        tileSelector.setButtonCell(new ComboBoxListCell<Tile>() {
            {
                super.setPrefWidth(125);
            }

            @Override
            public void updateItem(Tile item, boolean empty) {
                textProperty().unbind();
                super.updateItem(item, empty);
                if (item != null) {
                    textProperty().bind(tileNameField.textProperty());
                } else {
                    setText(null);
                }
            }
        });
        tileSelector.setCellFactory(new Callback<ListView<Tile>, ListCell<Tile>>() {
            @Override
            public ListCell<Tile> call(ListView<Tile> param) {
                return new EntitySelectorCell<Tile>(tileNameField) {
                    @Override
                    public void finishUpdate(Tile item) {
                        setGraphic(new ImageView(TileUtils.getImage(item, Application.currentPlatform)));
                    }
                };
            }
        });

        mapSelect.setButtonCell(new ComboBoxListCell<org.badvision.outlaweditor.data.xml.Map>() {
            {
                super.setPrefWidth(125);
            }

            @Override
            public void updateItem(org.badvision.outlaweditor.data.xml.Map item, boolean empty) {
                textProperty().unbind();
                super.updateItem(item, empty);
                if (item != null) {
                    textProperty().bind(mapNameField.textProperty());
                } else {
                    setText(null);
                }
            }
        });
        mapSelect.setCellFactory(new Callback<ListView<org.badvision.outlaweditor.data.xml.Map>, ListCell<org.badvision.outlaweditor.data.xml.Map>>() {
            @Override
            public ListCell<org.badvision.outlaweditor.data.xml.Map> call(ListView<org.badvision.outlaweditor.data.xml.Map> param) {
                return new EntitySelectorCell<org.badvision.outlaweditor.data.xml.Map>(mapNameField) {
                    @Override
                    public void finishUpdate(org.badvision.outlaweditor.data.xml.Map item) {
                    }
                };
            }
        });

        imageSelector.setButtonCell(new ComboBoxListCell<Image>() {
            {
                super.setPrefWidth(125);
            }

            @Override
            public void updateItem(Image item, boolean empty) {
                textProperty().unbind();
                super.updateItem(item, empty);
                if (item != null) {
                    textProperty().bind(imageNameField.textProperty());
                } else {
                    setText(null);
                }
            }
        });
        imageSelector.setCellFactory(new Callback<ListView<Image>, ListCell<Image>>() {
            @Override
            public ListCell<Image> call(ListView<Image> param) {
                return new EntitySelectorCell<Image>(imageNameField) {
                    @Override
                    public void finishUpdate(Image item) {
                    }
                };
            }
        });
    }

    @Override
    public void imageBitMode(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.setDrawMode(ImageEditor.DrawMode.Toggle);
        }
    }

    @Override
    public void imageDraw1BitMode(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.setDrawMode(ImageEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void imageDraw3BitMode(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.setDrawMode(ImageEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void imageDraw5BitMode(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.setDrawMode(ImageEditor.DrawMode.Pencil5px);
        }
    }

    @Override
    public void imageDrawFilledRectMode(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.setDrawMode(ImageEditor.DrawMode.Rectangle);
        }
    }

    @Override
    public void imageShift(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.showShiftUI();
        }
    }

    @Override
    public void imageTogglePanZoom(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.togglePanZoom();
        }
    }

    @Override
    public void imageZoomIn(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomIn();
        }
    }

    @Override
    public void imageZoomOut(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomOut();
        }
    }

    @Override
    public void mapDraw1(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.setDrawMode(MapEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void mapDraw3(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.setDrawMode(MapEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void mapDraw5(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.setDrawMode(MapEditor.DrawMode.Pencil5px);
        }
    }

    @Override
    public void mapDrawFilledRectMode(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.setDrawMode(MapEditor.DrawMode.FilledRect);
        }
    }

    @Override
    public void mapTogglePanZoom(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.togglePanZoom();
        }
    }

    @Override
    public void mapZoomIn(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.zoomIn();
        }
    }

    @Override
    public void mapZoomOut(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.zoomOut();
        }
    }

    @Override
    public void onCurrentTileSelected(ActionEvent event) {
        setCurrentTile(tileSelector.getSelectionModel().getSelectedItem());
    }

    @Override
    public void onChangePlatformAppleSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        Application.currentPlatform = Platform.AppleII;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        Application.currentPlatform = Platform.AppleII;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        Application.currentPlatform = Platform.AppleII_DHGR;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        Application.currentPlatform = Platform.AppleII_DHGR;
        platformChange();
    }

    private void platformChange() {
        for (Tile t : Application.gameData.getTile()) {
            TileUtils.redrawTile(t);
        }
        Tile tile = currentTile;
        rebuildTileSelectors();
        setCurrentTile(tile);
        if (currentMapEditor != null) {
            currentMapEditor.redraw();
        }
        rebuildImageSelector();
    }

    @Override
    public void onChangePlatformC64(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onEditCopy(ActionEvent event) {
        if (getVisibleEditor() != null) {
            getVisibleEditor().copy();
        }
    }

    @Override
    public void onEditPaste(ActionEvent event) {
        if (getVisibleEditor() != null) {
            getVisibleEditor().paste();
        }
    }

    @Override
    public void onEditSelect(ActionEvent event) {
        if (getVisibleEditor() != null) {
            getVisibleEditor().select();
        }
    }

    @Override
    public void onFileOpen(ActionEvent event) {
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Load);
            rebuildImageSelector();
            rebuildMapSelectors();
            rebuildTileSelectors();
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onFileQuit(ActionEvent event) {
        UIAction.quit();
    }

    @Override
    public void onFileSave(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.currentMap.updateBackingMap();
        }
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Save);
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onFileSaveAs(ActionEvent event) {
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Save_as);
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onHelpAbout(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onImageClonePressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onImageCreatePressed(ActionEvent event) {
        Image i = new Image();
        i.setName("Untitled");
        Application.gameData.getImage().add(i);
        setCurrentImage(i);
        rebuildImageSelector();
    }

    @Override
    public void onImageDeletePressed(ActionEvent event) {
        if (currentImage == null) {
            return;
        }
        confirm("Delete image '" + currentImage.getName() + "'.  Are you sure?", new Runnable() {
            @Override
            public void run() {
                Image del = currentImage;
                setCurrentImage(null);
                Application.gameData.getImage().remove(del);
                rebuildImageSelector();
            }
        }, null);
    }

    @Override
    public void onImageExportPressed(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.exportImage();
        }
    }

    @Override
    public void onImageSelected(ActionEvent event) {
        setCurrentImage(imageSelector.getSelectionModel().getSelectedItem());
    }

    @Override
    public void onMapClonePressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onMapCreatePressed(ActionEvent event) {
        org.badvision.outlaweditor.data.xml.Map m = new org.badvision.outlaweditor.data.xml.Map();
        m.setName("Untitled");
        gameData.getMap().add(m);
        m.setWidth(512);
        m.setHeight(512);
        setCurrentMap(m);
        rebuildMapSelectors();
    }

    @Override
    public void onMapDeletePressed(ActionEvent event) {
        if (currentMap == null) {
            return;
        }
        confirm("Delete map '" + currentMap.getName() + "'.  Are you sure?", new Runnable() {
            @Override
            public void run() {
                org.badvision.outlaweditor.data.xml.Map del = currentMap;
                setCurrentMap(null);
                Application.gameData.getMap().remove(del);
                rebuildMapSelectors();
            }
        }, null);
    }

    @Override
    public void onMapExportPressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onMapPreviewPressed(ActionEvent event) {
        if (currentMapEditor == null) {
            return;
        }
        currentMapEditor.showPreview();
    }

    @Override
    public void onMapScriptAddPressed(ActionEvent event) {
        createAndEditScript();
    }

    @Override
    public void onMapScriptClonePressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onMapScriptDeletePressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void onMapSelected(ActionEvent event) {
        setCurrentMap(mapSelect.getSelectionModel().getSelectedItem());
    }

    @Override
    public void onTileClonePressed(ActionEvent event) {
        if (currentTile == null) {
            return;
        }
        Tile t = new Tile();
        TileUtils.getId(t);
        t.setName(currentTile.getName() + " (clone)");
        t.setObstruction(currentTile.isObstruction());
        t.getCategory().addAll(currentTile.getCategory());
        for (PlatformData d : currentTile.getDisplayData()) {
            PlatformData p = new PlatformData();
            p.setHeight(d.getHeight());
            p.setWidth(d.getWidth());
            p.setPlatform(d.getPlatform());
            p.setValue(Arrays.copyOf(d.getValue(), d.getValue().length));
            t.getDisplayData().add(p);
        }
        TilesetUtils.add(t);
        rebuildTileSelectors();
        setCurrentTile(t);
    }

    @Override
    public void onTileCreatePressed(ActionEvent event) {
        Tile t = TileUtils.newTile();
        t.setName("Untitled");
        TilesetUtils.add(t);
        rebuildTileSelectors();
        setCurrentTile(t);
    }

    @Override
    public void onTileDeletePressed(ActionEvent event) {
        if (currentTile == null) {
            return;
        }
        confirm("Delete tile '" + currentTile.getName() + "'.  Are you sure?", new Runnable() {

            @Override
            public void run() {
                Tile del = currentTile;
                setCurrentTile(null);
                Application.gameData.getTile().remove(del);
                rebuildTileSelectors();
            }

        }, null);
    }

    @Override
    public void onTileExportPressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void scrollImageDown(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(0, 1);
        }
    }

    @Override
    public void scrollImageLeft(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(-1, 0);
        }
    }

    @Override
    public void scrollImageRight(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(1, 0);
        }
    }

    @Override
    public void scrollImageUp(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(0, -1);
        }
    }

    @Override
    public void scrollMapDown(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.scrollBy(0, 1);
        }
    }

    @Override
    public void scrollMapLeft(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.scrollBy(-1, 0);
        }
    }

    @Override
    public void scrollMapRight(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.scrollBy(1, 0);
        }
    }

    @Override
    public void scrollMapUp(ActionEvent event) {
        if (currentMapEditor != null) {
            currentMapEditor.scrollBy(0, -1);
        }
    }

    @Override
    public void tileBitMode(ActionEvent event) {
        if (currentTileEditor != null) {
            currentTileEditor.setDrawMode(TileEditor.DrawMode.Toggle);
        }
    }

    @Override
    public void tileDraw1BitMode(ActionEvent event) {
        if (currentTileEditor != null) {
            currentTileEditor.setDrawMode(TileEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void tileDraw3BitMode(ActionEvent event) {
        if (currentTileEditor != null) {
            currentTileEditor.setDrawMode(TileEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void tileShift(ActionEvent event) {
        if (currentTileEditor != null) {
            currentTileEditor.showShiftUI();
        }
    }

    private void setCurrentTileEditor(TileEditor editor) {
        if (editor != null) {
            editor.buildEditorUI(tileEditorAnchorPane);
            editor.buildPatternSelector(tilePatternMenu);
        }
        currentTileEditor = editor;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }

    public void setCurrentTile(Tile t) {
        tileSelector.getSelectionModel().select(t);
        if (t != null && t.equals(currentTile)) {
            return;
        }
        tileEditorAnchorPane.getChildren().clear();
        if (t == null) {
            bind(tileIdField.textProperty(), null);
            bind(tileCategoryField.textProperty(), null);
            bind(tileObstructionField.selectedProperty(), null);
            bind(tileNameField.textProperty(), null);
            tileIdField.setDisable(true);
            tileCategoryField.setDisable(true);
            tileObstructionField.setDisable(true);
            tileNameField.setDisable(true);
            setCurrentTileEditor(null);
        } else {
            if (t.isObstruction() == null) {
                t.setObstruction(false);
            }
            try {
                tileIdField.setDisable(false);
                tileCategoryField.setDisable(false);
                tileObstructionField.setDisable(false);
                tileNameField.setDisable(false);
                bind(tileIdField.textProperty(), stringProp(t, "id"));
                bind(tileCategoryField.textProperty(), categoryProp(t, "category"));
                bind(tileObstructionField.selectedProperty(), boolProp(t, "obstruction"));
                bind(tileNameField.textProperty(), stringProp(t, "name"));
                TileEditor editor = Application.currentPlatform.tileEditor.newInstance();
                editor.setEntity(t);
                setCurrentTileEditor(editor);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        currentTile = t;
    }

    public void rebuildTileSelectors() {
        tileSelector.getItems().clear();
        tileSelector.getItems().addAll(Application.gameData.getTile());
        tileSelector.getSelectionModel().select(getCurrentTile());
        mapSelectTile.getItems().clear();
        for (final Tile t : Application.gameData.getTile()) {
            WritableImage img = TileUtils.getImage(t, currentPlatform);
            ImageView iv = new ImageView(img);
            MenuItem mapSelectItem = new MenuItem(t.getName(), iv);
            mapSelectItem.setGraphic(new ImageView(TileUtils.getImage(t, currentPlatform)));
            mapSelectItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (currentMapEditor != null) {
                        currentMapEditor.setCurrentTile(t);
                    }
                }
            });
            mapSelectTile.getItems().add(mapSelectItem);
        }
    }

    public void setCurrentMap(org.badvision.outlaweditor.data.xml.Map m) {
        if (currentMap != null && currentMap.equals(m)) {
            return;
        }
//        mapEditorAnchorPane.getChildren().clear();
        currentMap = m;
        if (currentMapEditor != null) {
            currentMapEditor.unregister();
        }
        if (m == null) {
            bind(mapHeightField.textProperty(), null);
            bind(mapNameField.textProperty(), null);
            bind(mapWidthField.textProperty(), null);
            bind(mapWrapAround.selectedProperty(), null);
            mapHeightField.setDisable(true);
            mapNameField.setDisable(true);
            mapWidthField.setDisable(true);
            mapWrapAround.setDisable(true);
            currentMapEditor = null;
        } else {
            if (m.getHeight() == null) {
                m.setHeight(512);
            }
            if (m.getWidth() == null) {
                m.setWidth(512);
            }
            if (m.getName() == null) {
                m.setName("Untitled");
            }
            try {
                mapHeightField.setDisable(false);
                mapNameField.setDisable(false);
                mapWidthField.setDisable(false);
                mapWrapAround.setDisable(false);
//                bind(mapHeightField.textProperty(), intProp(m, "height"));
                bind(mapNameField.textProperty(), stringProp(m, "name"));
//                bind(mapWidthField.textProperty(), intProp(m, "width"));
//                bind(mapWrapAround.selectedProperty(),boolProp(m, "wrap"));
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            currentMapEditor = new MapEditor();
            currentMapEditor.setEntity(m);
            currentMapEditor.buildEditorUI(mapEditorAnchorPane);
        }
        redrawMapScripts();
    }

    public void rebuildMapSelectors() {
        mapSelect.getItems().clear();
        mapSelect.getItems().addAll(Application.gameData.getMap());
        mapSelect.getSelectionModel().select(getCurrentMap());
    }

    public org.badvision.outlaweditor.data.xml.Map getCurrentMap() {
        return currentMap;
    }

    private void setCurrentImage(Image i) {
        if (currentImage != null && currentImage.equals(i)) {
            return;
        }
        imageSelector.getSelectionModel().select(i);
        currentImage = i;
        if (currentImageEditor != null) {
            currentImageEditor.unregister();
        }
        if (i == null) {
            bind(imageCategoryField.textProperty(), null);
//            bind(imageHeightField.textProperty(), null);
            bind(imageNameField.textProperty(), null);
//            bind(imageWidthField.textProperty(),null);
            imageCategoryField.setDisable(true);
            imageHeightField.setDisable(true);
            imageNameField.setDisable(true);
            imageWidthField.setDisable(true);
            currentImageEditor = null;
        } else {
            if (i.getName() == null) {
                i.setName("Untitled");
            }
            try {
                imageCategoryField.setDisable(false);
                imageHeightField.setDisable(false);
                imageNameField.setDisable(false);
                imageWidthField.setDisable(false);
                bind(imageNameField.textProperty(), stringProp(i, "name"));
                bind(imageCategoryField.textProperty(), categoryProp(i, "category"));
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                currentImageEditor = currentPlatform.imageEditor.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            currentImageEditor.setEntity(i);
            currentImageEditor.buildEditorUI(imageEditorAnchorPane);
            currentImageEditor.buildPatternSelector(imagePatternMenu);
        }
    }

    private Image getCurrentImage() {
        return currentImage;
    }

    private void rebuildImageSelector() {
        Image i = getCurrentImage();
        imageSelector.getItems().clear();
        imageSelector.getItems().addAll(Application.gameData.getImage());
        imageSelector.getSelectionModel().select(i);
    }

    public static enum TABS {

        image, map, tile
    };
    TABS currentTab;

    @Override
    public void imageTabActivated(Event event) {
        currentTab = TABS.image;
    }

    @Override
    public void mapTabActivated(Event event) {
        currentTab = TABS.map;
    }

    @Override
    public void tileTabActivated(Event event) {
        currentTab = TABS.tile;
    }

    public Editor getVisibleEditor() {
        switch (currentTab) {
            case image:
                return currentImageEditor;
            case map:
                return currentMapEditor;
            case tile:
                return currentTileEditor;
        }
        return null;
    }

    public void redrawMapScripts() {
        mapScriptsList.setOnEditStart(new EventHandler<ListView.EditEvent<Script>>() {
            @Override
            public void handle(ListView.EditEvent<Script> event) {
                  UIAction.editScript(event.getSource().getItems().get(event.getIndex()));
            }
        });
        mapScriptsList.setCellFactory(new Callback<ListView<Script>, ListCell<Script>>() {
            @Override
            public ListCell<Script> call(ListView<Script> param) {
                return new ListCell<Script>() {
                    @Override
                    protected void updateItem(Script item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {                            
                            setText("");
                        } else {
                            setText(item.getName());
                        }
                    }
                };
            }
        });
        if (currentMap == null) {
            mapScriptsList.getItems().clear();
        } else {
            mapScriptsList.getItems().setAll(currentMap.getScripts().getScript());
        }
    }

    abstract public static class EntitySelectorCell<T> extends ComboBoxListCell<T> {

        static Map<TextField, Object> lastSelected = new HashMap<>();
        TextField nameField;

        public EntitySelectorCell(TextField tileNameField) {
            super.setPrefWidth(125);
            nameField = tileNameField;
        }

        @Override
        public void updateSelected(boolean sel) {
            if (sel) {
                Object o = lastSelected.get(nameField);
                if (o != null && !o.equals(getItem())) {
                    ((ListCell) o).updateSelected(false);
                }
                textProperty().unbind();
                textProperty().bind(nameField.textProperty());
                lastSelected.put(nameField, this);
            } else {
                updateItem(getItem(), false);
            }
        }

        @Override
        public void updateItem(T item, boolean empty) {
            textProperty().unbind();
            super.updateItem(item, empty);
            if (item != null && !(item instanceof String)) {
                try {
                    textProperty().bind(stringProp(item, "name"));
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
                finishUpdate(item);
            } else {
                setText(null);
            }
        }

        public void finishUpdate(T item) {
        }
    };
}
