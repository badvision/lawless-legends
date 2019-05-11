/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.ui.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.api.ApplicationState;
import static org.badvision.outlaweditor.data.PropertyHelper.bind;
import static org.badvision.outlaweditor.data.PropertyHelper.boolProp;
import static org.badvision.outlaweditor.data.PropertyHelper.stringProp;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.PlatformData;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.EntitySelectorCell;
import org.badvision.outlaweditor.ui.TileEditorTabController;
import org.badvision.outlaweditor.ui.UIAction;
import static org.badvision.outlaweditor.ui.UIAction.confirm;

/**
 * FXML Controller class for tile editor tab
 *
 * @author blurry
 */
public class TileEditorTabControllerImpl extends TileEditorTabController {
    FlowPane quickMenu = new FlowPane();
    ChangeListener rebuildListener = (ObservableValue value, Object oldValue, Object newValue) -> rebuildTileSelectors();

    @Override
    public void onCurrentTileSelected(ActionEvent event) {
        setCurrentTile(tileSelector.getSelectionModel().getSelectedItem());
    }

    @Override
    public void onTileClonePressed(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTile() == null) {
            return;
        }
        Tile t = new Tile();
        TileUtils.getId(t);
        t.setName(getCurrentTile().getName() + " (clone)");
        t.setObstruction(getCurrentTile().isObstruction());
        t.setSprite(getCurrentTile().isSprite());
        t.setBlocker(getCurrentTile().isBlocker());
        t.setCategory(getCurrentTile().getCategory());
        getCurrentTile().getDisplayData().stream().map((d) -> {
            PlatformData p = new PlatformData();
            p.setHeight(d.getHeight());
            p.setWidth(d.getWidth());
            p.setPlatform(d.getPlatform());
            p.setValue(Arrays.copyOf(d.getValue(), d.getValue().length));
            return p;
        }).forEach((p) -> {
            t.getDisplayData().add(p);
        });
        TilesetUtils.add(t);
        mainController.rebuildTileSelectors();
        setCurrentTile(t);
    }

    @Override
    public void onTileCreatePressed(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        Tile t = TileUtils.newTile();
        t.setName("Untitled");
        TilesetUtils.add(t);
        mainController.rebuildTileSelectors();
        setCurrentTile(t);
    }

    @Override
    public void onTileDeletePressed(ActionEvent event) {
        final ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTile() == null) {
            return;
        }
        confirm("Delete tile '" + getCurrentTile().getName() + "'.  Are you sure?", () -> {
            Tile del = getCurrentTile();
            setCurrentTile(null);
            ApplicationState.getInstance().getGameData().getTile().remove(del);
            mainController.rebuildTileSelectors();
        }, null);
    }

    @Override
    public void tileBitMode(ActionEvent event) {
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Toggle);
        }
    }

    @Override
    public void tileDraw1BitMode(ActionEvent event) {
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void tileDraw3BitMode(ActionEvent event) {
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void tileShift(ActionEvent event) {
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().showShiftUI();
        }
    }

    @Override
    public void onTileExportPressed(ActionEvent event) {
        getCurrentTileEditor().copy();
        UIAction.alert("Tile copied to the clipboard; use Paste Special in your target application to import this tile as an image, data, or a styled table (HTML)");
    }

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        assert tileCategoryField != null : "fx:id=\"tileCategoryField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileEditorAnchorPane != null : "fx:id=\"tileEditorAnchorPane\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileIdField != null : "fx:id=\"tileIdField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileNameField != null : "fx:id=\"tileNameField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileObstructionField != null : "fx:id=\"tileObstructionField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileSpriteField != null : "fx:id=\"tileSpriteField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileBlockerField != null : "fx:id=\"tileBlockerField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tilePatternMenu != null : "fx:id=\"tilePatternMenu\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileSelector != null : "fx:id=\"tileSelector\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        tileSelector.setCellFactory((ListView<Tile> param) -> {
            return new EntitySelectorCell<Tile>(tileNameField, tileCategoryField) {
                @Override
                public void finishUpdate(Tile item) {
                    setGraphic(new ImageView(TileUtils.getImage(item, ApplicationState.getInstance().getCurrentPlatform())));
                }
            };
        });
        tileSelector.setConverter(new StringConverter<Tile>() {
            @Override
            public String toString(Tile object) {
                return String.valueOf(object.getCategory() + "/" + object.getName());
            }

            @Override
            public Tile fromString(String string) {
                return null;
            }
        });
    }

    @Override
    public void setCurrentTileEditor(TileEditor editor) {
        if (editor != null) {
            editor.buildEditorUI(tileEditorAnchorPane);
            editor.buildPatternSelector(tilePatternMenu);
        }
        super.setCurrentTileEditor(editor);
    }

    @Override
    public void setCurrentTile(Tile t) {
        tileNameField.textProperty().removeListener(rebuildListener);
        tileCategoryField.textProperty().removeListener(rebuildListener);
        tileSelector.getSelectionModel().select(t);
        if (t != null && t.equals(getCurrentTile())) {
            return;
        }
        tileEditorAnchorPane.getChildren().clear();
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().unregister();
        }
        if (t == null) {
            bind(tileIdField.textProperty(), null);
            bind(tileCategoryField.textProperty(), null);
            bind(tileObstructionField.selectedProperty(), null);
            bind(tileSpriteField.selectedProperty(), null);
            bind(tileBlockerField.selectedProperty(), null);
            bind(tileNameField.textProperty(), null);

            tileIdField.setDisable(true);
            tileCategoryField.setDisable(true);
            tileObstructionField.setDisable(true);
            tileSpriteField.setDisable(true);
            tileBlockerField.setDisable(true);
            tileNameField.setDisable(true);
            setCurrentTileEditor(null);
        } else {
            if (t.isObstruction() == null) {
                t.setObstruction(false);
            }
            if (t.isSprite() == null) {
                t.setSprite(false);
            }
            if (t.isBlocker() == null) {
                t.setBlocker(false);
            }
            try {
                tileIdField.setDisable(false);
                tileCategoryField.setDisable(false);
                tileObstructionField.setDisable(false);
                tileSpriteField.setDisable(false);
                tileBlockerField.setDisable(false);
                tileNameField.setDisable(false);
                bind(tileIdField.textProperty(), stringProp(t, "id"));
                bind(tileCategoryField.textProperty(), stringProp(t, "category"));
                bind(tileObstructionField.selectedProperty(), boolProp(t, "obstruction"));
                bind(tileSpriteField.selectedProperty(), boolProp(t, "sprite"));
                bind(tileBlockerField.selectedProperty(), boolProp(t, "blocker"));
                bind(tileNameField.textProperty(), stringProp(t, "name"));
                TileEditor editor = ApplicationState.getInstance().getCurrentPlatform().tileEditor.newInstance();
                editor.setEntity(t);
                setCurrentTileEditor(editor);
                tileNameField.textProperty().addListener(rebuildListener);
                tileCategoryField.textProperty().addListener(rebuildListener);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        super.setCurrentTile(t);
    }

    @Override
    public void rebuildTileSelectors() {
        Tile t = getCurrentTile();
        tileSelector.getItems().clear();
        List<Tile> allTiles = ApplicationState.getInstance().getGameData().getTile();
        allTiles.sort(Comparator.comparing((Tile o) -> String.valueOf(o.getCategory())).thenComparing(o -> String.valueOf(o.getName())));
        tileSelector.getItems().addAll(allTiles);
        tileSelector.getSelectionModel().select(allTiles.indexOf(getCurrentTile()));
        setCurrentTile(t);
    }
}
