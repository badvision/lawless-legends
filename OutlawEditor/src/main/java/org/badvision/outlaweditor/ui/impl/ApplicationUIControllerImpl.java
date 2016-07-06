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

import javafx.event.Event;
import javafx.scene.input.DataFormat;
import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.api.ApplicationState;
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

        TilesetUtils.addObserver((Object object) -> {
            rebuildTileSelectors();
        });
        tileController.initalize();
        mapController.initalize();
        imageController.initalize();
        globalController.initialize();
        menuController.initalize();
    }

    @Override
    public void platformChange() {
        ApplicationState.getInstance().getGameData().getTile().stream().forEach((t) -> {
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
        globalController.redrawGlobalDataTypes();
        globalController.redrawGlobalVariables();
        globalController.redrawGlobalSheets();
    }

    @Override
    public void rebuildTileSelectors() {
        tileController.rebuildTileSelectors();
        mapController.rebuildTileSelectors();
    }

    @Override
    public void redrawScripts() {
        mapController.redrawMapScripts();
        globalController.redrawGlobalScripts();
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
        image, map, tile, global
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
    public void globalTabActivated(Event event) {
        currentTab = TABS.global;
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
            case global:
                return globalController.getCurrentEditor();
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
