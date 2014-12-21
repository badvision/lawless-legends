package org.badvision.outlaweditor.ui.impl;

import org.badvision.outlaweditor.ui.EntitySelectorCell;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.ImageEditor;
import static org.badvision.outlaweditor.Application.currentPlatform;
import static org.badvision.outlaweditor.ui.UIAction.confirm;
import static org.badvision.outlaweditor.data.PropertyHelper.bind;
import static org.badvision.outlaweditor.data.PropertyHelper.categoryProp;
import static org.badvision.outlaweditor.data.PropertyHelper.stringProp;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.ui.ImageEditorTabController;

/**
 * FXML Controller class
 *
 * @author blurry
 */
public class ImageEditorTabControllerImpl extends ImageEditorTabController {

    public Image currentImage = null;
    public ImageEditor currentImageEditor = null;

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        super.initalize();
        imageSelector.setButtonCell(new ComboBoxListCell<Image>() {
            {
                super.setPrefWidth(125);
            }

            @Override
            public void updateItem(Image item, boolean empty) {
                textProperty().unbind();
                super.updateItem(item, empty);
                if (item != null) {
                    textProperty().bind(imageNameField.textProperty());
                } else {
                    setText(null);
                }
            }
        });
        imageSelector.setCellFactory((ListView<Image> param) -> new EntitySelectorCell<Image>(imageNameField) {
            @Override
            public void finishUpdate(Image item) {
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

    @Override
    public void imageZoomIn(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomIn();
            updateScrollAreaWithScale(currentImageEditor.getZoomScale());
        }
    }

    @Override
    public void imageZoomOut(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomOut();
            updateScrollAreaWithScale(currentImageEditor.getZoomScale());
        }
    }

    @Override
    public void onImageClonePressed(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        imageSelector.getSelectionModel().select(i);
        currentImage = i;
        if (currentImageEditor != null) {
            currentImageEditor.unregister();
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
                bind(imageCategoryField.textProperty(), categoryProp(i, "category"));
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
            imageEditorZoomGroup.setScaleX(1.0);
            imageEditorZoomGroup.setScaleY(1.0);
        }
    }

    private Image getCurrentImage() {
        return currentImage;
    }

    @Override
    public void rebuildImageSelector() {
        Image i = getCurrentImage();
        imageSelector.getItems().clear();
        imageSelector.getItems().addAll(Application.gameData.getImage());
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
