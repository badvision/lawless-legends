package org.badvision.outlaweditor.ui.impl;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.TileEditor;
import static org.badvision.outlaweditor.ui.UIAction.confirm;
import static org.badvision.outlaweditor.data.PropertyHelper.bind;
import static org.badvision.outlaweditor.data.PropertyHelper.boolProp;
import static org.badvision.outlaweditor.data.PropertyHelper.categoryProp;
import static org.badvision.outlaweditor.data.PropertyHelper.stringProp;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.PlatformData;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.TileEditorTabController;

/**
 * FXML Controller class for tile editor tab
 *
 * @author blurry
 */
public class TileEditorTabControllerImpl extends TileEditorTabController {   
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
        t.getCategory().addAll(getCurrentTile().getCategory());
        for (PlatformData d : getCurrentTile().getDisplayData()) {
            PlatformData p = new PlatformData();
            p.setHeight(d.getHeight());
            p.setWidth(d.getWidth());
            p.setPlatform(d.getPlatform());
            p.setValue(Arrays.copyOf(d.getValue(), d.getValue().length));
            t.getDisplayData().add(p);
        }
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
        confirm("Delete tile '" + getCurrentTile().getName() + "'.  Are you sure?", new Runnable() {

            @Override
            public void run() {
                Tile del = getCurrentTile();
                setCurrentTile(null);
                Application.gameData.getTile().remove(del);
                mainController.rebuildTileSelectors();
            }

        }, null);
    }

    @Override
    public void tileBitMode(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Toggle);
        }
    }

    @Override
    public void tileDraw1BitMode(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Pencil1px);
        }
    }

    @Override
    public void tileDraw3BitMode(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().setDrawMode(TileEditor.DrawMode.Pencil3px);
        }
    }

    @Override
    public void tileShift(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (getCurrentTileEditor() != null) {
            getCurrentTileEditor().showShiftUI();
        }
    }

    @Override
    public void onTileExportPressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        assert tilePatternMenu != null : "fx:id=\"tilePatternMenu\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileSelector != null : "fx:id=\"tileSelector\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        
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
                return new ApplicationUIControllerImpl.EntitySelectorCell<Tile>(tileNameField) {
                    @Override
                    public void finishUpdate(Tile item) {
                        setGraphic(new ImageView(TileUtils.getImage(item, Application.currentPlatform)));
                    }
                };
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
        tileSelector.getSelectionModel().select(t);
        if (t != null && t.equals(getCurrentTile())) {
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
        super.setCurrentTile(t);
    }

    @Override
    public void rebuildTileSelectors() {
        tileSelector.getItems().clear();
        tileSelector.getItems().addAll(Application.gameData.getTile());
        tileSelector.getSelectionModel().select(getCurrentTile());
    }
}
