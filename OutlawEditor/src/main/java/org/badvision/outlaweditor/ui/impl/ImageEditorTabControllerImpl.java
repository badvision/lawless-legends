package org.badvision.outlaweditor.ui.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.util.Callback;
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
        imageSelector.setCellFactory(new Callback<ListView<Image>, ListCell<Image>>() {
            @Override
            public ListCell<Image> call(ListView<Image> param) {
                return new ApplicationUIControllerImpl.EntitySelectorCell<Image>(imageNameField) {
                    @Override
                    public void finishUpdate(Image item) {
                    }
                };
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
        }
    }

    @Override
    public void imageZoomOut(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.zoomOut();
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
        confirm("Delete image '" + currentImage.getName() + "'.  Are you sure?", new Runnable() {
            @Override
            public void run() {
                Image del = currentImage;
                setCurrentImage(null);
                Application.gameData.getImage().remove(del);
                rebuildImageSelector();
            }
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

    @Override
    public void scrollImageDown(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(0, 1);
        }
    }

    @Override
    public void scrollImageLeft(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(-1, 0);
        }
    }

    @Override
    public void scrollImageRight(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(1, 0);
        }
    }

    @Override
    public void scrollImageUp(ActionEvent event) {
        if (currentImageEditor != null) {
            currentImageEditor.scrollBy(0, -1);
        }
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
            currentImageEditor.buildEditorUI(imageEditorAnchorPane);
            currentImageEditor.buildPatternSelector(imagePatternMenu);
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
}
