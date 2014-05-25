package org.badvision.outlaweditor.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 *
 * @author blurry
 */
public abstract class ApplicationMenuController {

    @FXML
    abstract public void onChangePlatformAppleDHGRSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleDHGRText(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleText(ActionEvent event);

    @FXML
    abstract public void onChangePlatformC64(ActionEvent event);

    @FXML
    abstract public void onEditCopy(ActionEvent event);

    @FXML
    abstract public void onEditPaste(ActionEvent event);

    @FXML
    abstract public void onEditSelect(ActionEvent event);

    @FXML
    abstract public void onFileOpen(ActionEvent event);

    @FXML
    abstract public void onFileQuit(ActionEvent event);

    @FXML
    abstract public void onFileSave(ActionEvent event);

    @FXML
    abstract public void onFileSaveAs(ActionEvent event);

    @FXML
    abstract public void onHelpAbout(ActionEvent event);

}
