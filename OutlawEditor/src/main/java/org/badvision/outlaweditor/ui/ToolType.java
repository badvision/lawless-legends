/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.ui;

import java.util.Optional;

import javafx.scene.image.Image;

public enum ToolType {
    ERASER("/images/eraser.png"), FILL(null), SELECT(null), MOVE(null), DRAW(null);

    ToolType(String iconPath) {
        if (iconPath != null) {
            icon = Optional.of(new Image(ToolType.class.getResourceAsStream(iconPath)));
        } else {
            icon = Optional.empty();
        }
    }
    Optional<Image> icon;
    public Optional<Image> getIcon() {
        return icon;
    }
}
