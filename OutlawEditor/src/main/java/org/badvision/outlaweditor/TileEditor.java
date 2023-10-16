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

import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.PatternSelectModal;

import javafx.scene.control.Menu;
import javafx.scene.layout.Pane;

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
    
    @Override
    public void copyEntityFrom(Tile src) {
        Tile dest = getEntity();
        dest.setBlocker(src.isBlocker());
        dest.setCategory(src.getCategory());
        dest.setComment(src.getComment());
        dest.setId(src.getId());
        dest.setName(src.getName());
        dest.setObstruction(src.isObstruction());
        dest.setSprite(src.isSprite());
        dest.getDisplayData().clear();
        dest.getDisplayData().addAll(src.getDisplayData());
    }

}
