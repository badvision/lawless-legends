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

import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.util.StringConverter;
import javax.xml.bind.JAXBException;
import org.badvision.outlaweditor.Application;
import static org.badvision.outlaweditor.Application.currentPlatform;
import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.ImageEditor;
import org.badvision.outlaweditor.TransferHelper;
import static org.badvision.outlaweditor.data.PropertyHelper.bind;
import static org.badvision.outlaweditor.data.PropertyHelper.stringProp;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.ui.EntitySelectorCell;
import org.badvision.outlaweditor.ui.ImageEditorTabController;
import static org.badvision.outlaweditor.ui.UIAction.confirm;

/**
 * FXML Controller class
 *
 * @author blurry
 */
public class ImageEditorTabControllerImpl extends ImageEditorTabController {

    public Image currentImage = null;
    public ImageEditor currentImageEditor = null;
    ChangeListener rebuildListener = (ObservableValue value, Object oldValue, Object newValue) -> rebuildImageSelector();

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        super.initalize();
        imageSelector.setCellFactory((ListView<Image> param) -> new EntitySelectorCell<Image>(imageNameField, imageCategoryField) {
            @Override
            public void finishUpdate(Image item) {
            }
        });
        imageSelector.setConverter(new StringConverter<Image>() {
            @Override
            public String toString(Image object) {
                return String.valueOf(object.getCategory()) + "/" + String.valueOf(object.getName());
            }

            @Override
            public Image fromString(String string) {
                return null;
            }
        });
    }

    @Override
    public Editor getCurrentEditor() {
        return currentImageEditor;
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

    private void updateZoomLabel() {
        zoomLabel.setText(String.format("%1.1fx", currentImageEditor.getZoomScale()));
    }

    @Override
    public void imageZoomIn(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomIn();
            updateZoomLabel();
            updateScrollAreaWithScale(currentImageEditor.getZoomScale());
        }
    }

    @Override
    public void imageZoomOut(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomOut();
            updateZoomLabel();
            updateScrollAreaWithScale(currentImageEditor.getZoomScale());
        }
    }

    @Override
    public void onImageClonePressed(ActionEvent event) {
        try {
            if (getCurrentImage() == null) {
                return;
            }
            Image clone = TransferHelper.cloneObject(getCurrentImage(), Image.class, "image");
            clone.setName(clone.getName()+" clone");
            Application.gameData.getImage().add(clone);
            setCurrentImage(clone);
            rebuildImageSelector();
        } catch (JAXBException ex) {
            Logger.getLogger(ImageEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        confirm("Delete image '" + currentImage.getName() + "'.  Are you sure?", () -> {
            Image del = currentImage;
            setCurrentImage(null);
            Application.gameData.getImage().remove(del);
            rebuildImageSelector();
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

    private void setCurrentImage(Image i) {
        if (currentImage != null && currentImage.equals(i)) {
            return;
        }
        imageNameField.textProperty().removeListener(rebuildListener);
        imageCategoryField.textProperty().removeListener(rebuildListener);
        imageSelector.getSelectionModel().select(i);
        currentImage = i;
        EnumMap oldEditorState = null;
        if (currentImageEditor != null) {
            oldEditorState = currentImageEditor.getState();
            currentImageEditor.unregister();
            cursorInfo.textProperty().unbind();
            cursorInfo.setText("");
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
            cursorInfo.textProperty().unbind();
            cursorInfo.setText("");
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
                bind(imageCategoryField.textProperty(), stringProp(i, "category"));
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                currentImageEditor = currentPlatform.imageEditor.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            currentImageEditor.setEntity(i);
            currentImageEditor.buildEditorUI(imageEditorScrollAnchorPane);
            currentImageEditor.buildPatternSelector(imagePatternMenu);
//            imageEditorZoomGroup.setScaleX(1.0);
//            imageEditorZoomGroup.setScaleY(1.0);
            imageNameField.textProperty().addListener(rebuildListener);
            imageCategoryField.textProperty().addListener(rebuildListener);
            if (oldEditorState != null) {
                currentImageEditor.setState(oldEditorState);
            }
        }
        cursorInfo.textProperty().bind(currentImageEditor.cursorInfoProperty());
    }

    private Image getCurrentImage() {
        return currentImage;
    }

    @Override
    public void rebuildImageSelector() {
        Image i = getCurrentImage();
        imageSelector.getItems().clear();
        List<Image> allImages = Application.gameData.getImage();
        allImages.sort((Image o1, Image o2) -> {
            int c1 = String.valueOf(o1.getCategory()).compareTo(String.valueOf(o2.getCategory()));
            if (c1 != 0) {
                return c1;
            }
            return String.valueOf(o1.getName()).compareTo(String.valueOf(o2.getName()));
        });

        imageSelector.getItems().addAll(allImages);
        imageSelector.getSelectionModel().select(i);
    }

    private void updateScrollAreaWithScale(double zoomScale) {
        double hval = imageEditorScrollPane.getHvalue();
        double vval = imageEditorScrollPane.getVvalue();
        imageEditorZoomGroup.setScaleX(zoomScale);
        imageEditorZoomGroup.setScaleY(zoomScale);
        imageEditorScrollPane.setHvalue(hval);
        imageEditorScrollPane.setVvalue(vval);
    }
}
