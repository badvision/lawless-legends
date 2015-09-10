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

import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.data.TileMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * @author brobert
 */
public abstract class ImageRenderer {

    public abstract WritableImage renderImage(WritableImage img, byte[] rawImage, int width, int height);

    public abstract byte[] createImageBuffer(int width, int height);

    public abstract byte[] renderPreview(TileMap map, int startX, int startY, int width, int height);

    public abstract WritableImage renderScanline(WritableImage currentImage, int y, int width, byte[] imageData);

}
