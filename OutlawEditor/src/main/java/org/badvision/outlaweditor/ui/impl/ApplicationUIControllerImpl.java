package org.badvision.outlaweditor.ui.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.Event;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.DataFormat;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.Editor;
import static org.badvision.outlaweditor.data.PropertyHelper.*;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.ApplicationUIController;

/**
 * Actual implementation of Application UI, isolated from auto-generated code
 *
 * @author brobert
 */
public class ApplicationUIControllerImpl extends ApplicationUIController {

    @Override
    public void initialize() {
        super.initialize();

        TilesetUtils.addObserver(new org.badvision.outlaweditor.data.DataObserver() {
            @Override
            public void observedObjectChanged(Object object) {
                rebuildTileSelectors();
            }
        });
    }

    @Override
    public void platformChange() {
        for (Tile t : Application.gameData.getTile()) {
            TileUtils.redrawTile(t);
        }
        Tile tile = tileController.getCurrentTile();
        rebuildTileSelectors();
        tileController.setCurrentTile(tile);
        rebuildImageSelectors();
        redrawAllTabs();
    }

    public void redrawAllTabs() {
        if (mapController.getCurrentEditor() != null) {
            mapController.getCurrentEditor().redraw();
        }
        if (imageController.getCurrentEditor() != null) {
            imageController.getCurrentEditor().redraw();
        }
    }

    @Override
    public void updateSelectors() {
        rebuildImageSelectors();
        rebuildMapSelectors();
        rebuildTileSelectors();
        redrawScripts();
    }

    @Override
    public void rebuildTileSelectors() {
        tileController.rebuildTileSelectors();
        mapController.rebuildTileSelectors();
    }

    @Override
    public void redrawScripts() {
        mapController.redrawMapScripts();
    }

    @Override
    public void rebuildMapSelectors() {
        mapController.rebuildMapSelectors();
    }

    @Override
    public void rebuildImageSelectors() {
        imageController.rebuildImageSelector();
    }

    @Override
    public void completeInflightOperations() {
        mapController.completeInflightOperations();
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
                return imageController.getCurrentEditor();
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

    @Override
    public void clearData() {
        tileController.setCurrentTile(null);
        mapController.setCurrentMap(null);
        tileController.setCurrentTile(null);
    }

}
