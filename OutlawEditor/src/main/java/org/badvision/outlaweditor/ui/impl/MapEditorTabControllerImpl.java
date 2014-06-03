package org.badvision.outlaweditor.ui.impl;

import org.badvision.outlaweditor.ui.EntitySelectorCell;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.MapEditor;
import org.badvision.outlaweditor.TransferHelper;
import static org.badvision.outlaweditor.Application.currentPlatform;
import static org.badvision.outlaweditor.Application.gameData;
import static org.badvision.outlaweditor.ui.UIAction.confirm;
import static org.badvision.outlaweditor.ui.UIAction.createAndEditScript;
import static org.badvision.outlaweditor.data.PropertyHelper.bind;
import static org.badvision.outlaweditor.data.PropertyHelper.stringProp;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.ui.MapEditorTabController;
import org.badvision.outlaweditor.ui.UIAction;

/**
 *
 * @author blurry
 */
public class MapEditorTabControllerImpl extends MapEditorTabController {

    @Override
    public void mapDraw1(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().setDrawMode(MapEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void mapDraw3(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().setDrawMode(MapEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void mapDraw5(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().setDrawMode(MapEditor.DrawMode.Pencil5px);
        }
    }

    @Override
    public void mapDrawFilledRectMode(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().setDrawMode(MapEditor.DrawMode.FilledRect);
        }
    }

    @Override
    public void mapTogglePanZoom(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().togglePanZoom();
        }
    }

    @Override
    public void mapZoomIn(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().zoomIn();
        }
    }

    @Override
    public void mapZoomOut(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().zoomOut();
        }
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
        final Map currentMap = getCurrentMap();
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
        if (getCurrentEditor() == null) {
            return;
        }
        getCurrentEditor().showPreview();
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
    public void scrollMapDown(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().scrollBy(0, 1);
        }
    }

    @Override
    public void scrollMapLeft(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().scrollBy(-1, 0);
        }
    }

    @Override
    public void scrollMapRight(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().scrollBy(1, 0);
        }
    }

    @Override
    public void scrollMapUp(ActionEvent event) {
        if (getCurrentEditor() != null) {
            getCurrentEditor().scrollBy(0, -1);
        }
    }

    @Override
    public Map getCurrentMap() {
        if (getCurrentEditor() == null) {
            return null;
        } else {
            return getCurrentEditor().getCurrentMap().getBackingMap();
        }
    }

    @Override
    public void completeInflightOperations() {
        if (getCurrentEditor() != null) {
            getCurrentEditor().getCurrentMap().updateBackingMap();
        }
    }

    @Override
    public void setCurrentMap(Map m) {
        if (getCurrentMap() != null && getCurrentMap().equals(m)) {
            return;
        }
//        mapEditorAnchorPane.getChildren().clear();
        if (getCurrentEditor() != null) {
            getCurrentEditor().unregister();
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
            setCurrentEditor(null);
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
            MapEditor e = new MapEditor();
            e.setEntity(m);
            e.buildEditorUI(mapEditorAnchorPane);
            setCurrentEditor(e);
        }
        redrawMapScripts();
    }

    @Override
    public void rebuildMapSelectors() {
        mapSelect.getItems().clear();
        mapSelect.getItems().addAll(Application.gameData.getMap());
        mapSelect.getSelectionModel().select(getCurrentMap());
    }

    @Override
    public void initalize() {
        super.initalize();
        mapSelect.setButtonCell(new ComboBoxListCell<org.badvision.outlaweditor.data.xml.Map>() {
            {
                super.setPrefWidth(125);
            }

            @Override
            public void updateItem(Map item, boolean empty) {
                textProperty().unbind();
                super.updateItem(item, empty);
                if (item != null) {
                    textProperty().bind(mapNameField.textProperty());
                } else {
                    setText(null);
                }
            }
        });
        mapSelect.setCellFactory(new Callback<ListView<Map>, ListCell<Map>>() {
            @Override
            public ListCell<org.badvision.outlaweditor.data.xml.Map> call(ListView<Map> param) {
                return new EntitySelectorCell<Map>(mapNameField) {
                    @Override
                    public void finishUpdate(Map item) {
                        }
                };
            }
        });
    }

    @Override
    public void rebuildTileSelectors() {
        mapSelectTile.getItems().clear();
        for (final Tile t : Application.gameData.getTile()) {
            WritableImage img = TileUtils.getImage(t, currentPlatform);
            ImageView iv = new ImageView(img);
            MenuItem mapSelectItem = new MenuItem(t.getName(), iv);
            mapSelectItem.setGraphic(new ImageView(TileUtils.getImage(t, currentPlatform)));
            mapSelectItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (getCurrentEditor() != null) {
                        getCurrentEditor().setCurrentTile(t);
                    }
                }
            });
            mapSelectTile.getItems().add(mapSelectItem);
        }
    }

    @Override
    public void redrawMapScripts() {
        mapScriptsList.setOnEditStart(new EventHandler<ListView.EditEvent<Script>>() {
            @Override
            public void handle(ListView.EditEvent<Script> event) {
                UIAction.editScript(event.getSource().getItems().get(event.getIndex()));
            }
        });
        final TransferHelper<Script> scriptDragDrop = new TransferHelper<>(Script.class);
        mapScriptsList.setCellFactory(new Callback<ListView<Script>, ListCell<Script>>() {
            @Override
            public ListCell<Script> call(ListView<Script> param) {
                final ListCell<Script> cell = new ListCell<Script>() {
                    @Override
                    protected void updateItem(Script item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText(item.getName());
                            scriptDragDrop.registerDragSupport(this, item);
                        }
                    }
                };
                return cell;
            }
        });
        if (getCurrentMap() == null) {
            mapScriptsList.getItems().clear();
        } else {
            if (mapScriptsList.getItems() != null && getCurrentMap().getScripts() != null) {
                mapScriptsList.getItems().setAll(getCurrentMap().getScripts().getScript());
            } else {
                mapScriptsList.getItems().clear();
            }
        }
    }
}
