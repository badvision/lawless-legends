package org.badvision.outlaweditor.ui.impl;

import javafx.event.Event;
import javafx.scene.input.DataFormat;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.Editor;
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

        TilesetUtils.addObserver((org.badvision.outlaweditor.data.DataObserver) (Object object) -> {
            rebuildTileSelectors();
        });
        tileController.initalize();
        mapController.initalize();
        imageController.initalize();
    }

    @Override
    public void platformChange() {
        Application.gameData.getTile().stream().forEach((t) -> {
            TileUtils.redrawTile(t);
        });
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
        image, map, tile, scripting
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
    public void scriptTabActivated(Event event) {
        currentTab = TABS.scripting;
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
            default:
                return null;
        }
    }

    public static final DataFormat SCRIPT_DATA_FORMAT = new DataFormat("MythosScript");

    @Override
    public void clearData() {
        tileController.setCurrentTile(null);
        mapController.setCurrentMap(null);
        tileController.setCurrentTile(null);
    }

}
