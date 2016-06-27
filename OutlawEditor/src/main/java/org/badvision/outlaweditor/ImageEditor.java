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

import org.badvision.outlaweditor.api.Platform;
import java.util.EnumMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;

/**
 *
 * @author brobert
 */
public abstract class ImageEditor extends Editor<Image, ImageEditor.DrawMode> {

    public static enum DrawMode {

        Toggle, Pencil1px, Pencil3px, Pencil5px, Rectangle, Circle, Stamp, Select
    }

    abstract public EnumMap getState();
    
    abstract public void setState(EnumMap oldState);
    
    abstract public void buildPatternSelector(Menu tilePatternMenu);

    public abstract void togglePanZoom();

    public abstract void zoomIn();

    public abstract void zoomOut();
    
    public abstract double getZoomScale();

    public abstract void exportImage();

    public abstract void resize(int newWidth, int newHeight);

    public PlatformData getPlatformData(Platform p) {
        for (PlatformData data : getEntity().getDisplayData()) {
            if (data.getPlatform().equalsIgnoreCase(p.name())) {
                return data;
            }
        }
        return null;
    }

    StringProperty cursorInfo = new SimpleStringProperty();
    public StringProperty cursorInfoProperty() {
        return cursorInfo;
    }
}
