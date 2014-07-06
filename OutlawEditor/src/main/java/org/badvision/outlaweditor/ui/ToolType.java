package org.badvision.outlaweditor.ui;

import java.util.Optional;
import javafx.scene.image.Image;

public enum ToolType {
    ERASER("images/eraser.png"), FILL(null), SELECT(null), MOVE(null), DRAW(null);

    ToolType(String iconPath) {
        if (iconPath != null) {
            icon = Optional.of(new Image(iconPath));
        } else {
            icon = Optional.empty();
        }
    }
    Optional<Image> icon;
    public Optional<Image> getIcon() {
        return icon;
    }
}
