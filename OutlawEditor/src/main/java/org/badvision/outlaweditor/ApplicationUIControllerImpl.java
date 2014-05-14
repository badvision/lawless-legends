package org.badvision.outlaweditor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.DataFormat;
import javafx.util.Callback;
import static org.badvision.outlaweditor.Application.currentPlatform;
import static org.badvision.outlaweditor.UIAction.*;
import static org.badvision.outlaweditor.data.PropertyHelper.*;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 * Actual implementation of Application UI, isolated from auto-generated code
 *
 * @author brobert
 */
public class ApplicationUIControllerImpl extends ApplicationUIController {

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
    public void platformChange() {
        for (Tile t : Application.gameData.getTile()) {
            TileUtils.redrawTile(t);
        }
        Tile tile = tileController.getCurrentTile();
        rebuildTileSelectors();
        tileController.setCurrentTile(tile);
        if (mapController.getCurrentEditor() != null) {
            mapController.getCurrentEditor().redraw();
        }
        rebuildImageSelector();
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
    public void rebuildTileSelectors() {
        tileController.rebuildTileSelectors();
        mapController.rebuildTileSelectors();
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

    @Override
    public void rebuildImageSelector() {
        Image i = getCurrentImage();
        imageSelector.getItems().clear();
        imageSelector.getItems().addAll(Application.gameData.getImage());
        imageSelector.getSelectionModel().select(i);
    }

    @Override
    public void completeInflightOperations() {
        if (mapController.getCurrentEditor() != null) {
            mapController.getCurrentEditor().currentMap.updateBackingMap();
        }
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

    @Override
    public Editor getVisibleEditor() {
        switch (currentTab) {
            case image:
                return currentImageEditor;
            case map:
                return mapController.getCurrentEditor();
            case tile:
                return tileController.getCurrentTileEditor();
        }
        return null;
    }

    public static final DataFormat SCRIPT_DATA_FORMAT = new DataFormat("MythosScript");

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
