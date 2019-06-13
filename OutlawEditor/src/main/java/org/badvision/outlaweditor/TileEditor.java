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

/**
 *
 * @author brobert
 */
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.PatternSelectModal;
import org.badvision.outlaweditor.ui.TileSelectModal;

public abstract class TileEditor extends Editor<Tile, TileEditor.DrawMode> {
    abstract public void buildPatternSelector(Menu tilePatternMenu);

    public static enum DrawMode {

        Pencil1px, Pencil3px, Toggle
    }

    private Pane targetPane;
    private PatternSelectModal patternSelectModal;

    public void registerPatternSelectorModal(Pane pane, PatternSelectModal modal) {
        this.targetPane = pane;
        patternSelectModal = modal;
    }

    @Override
    public void showSelectorModal() {
        patternSelectModal.showPatternSelectModal(targetPane);
    }
}
