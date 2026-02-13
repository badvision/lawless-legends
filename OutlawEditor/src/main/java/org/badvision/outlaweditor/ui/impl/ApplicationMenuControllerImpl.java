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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.apple.AppleTileRenderer;
import org.badvision.outlaweditor.data.FlagAuditor;
import org.badvision.outlaweditor.ui.ApplicationMenuController;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.FlagAuditDialogController;
import org.badvision.outlaweditor.ui.UIAction;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
// import org.osgi.framework.BundleContext;
// import org.osgi.framework.InvalidSyntaxException;

/**
 *
 * @author blurry
 */
public class ApplicationMenuControllerImpl extends ApplicationMenuController {
    @Override
    public void initalize() {
    }

    @Override
    public void onChangePlatformAppleSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        ApplicationState.getInstance().setCurrentPlatform(Platform.AppleII);
        ApplicationUIController.getController().platformChange();
    }

    @Override
    public void onChangePlatformAppleText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        ApplicationState.getInstance().setCurrentPlatform(Platform.AppleII);
        ApplicationUIController.getController().platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        ApplicationState.getInstance().setCurrentPlatform(Platform.AppleII_DHGR);
        ApplicationUIController.getController().platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        ApplicationState.getInstance().setCurrentPlatform(Platform.AppleII_DHGR);
        ApplicationUIController.getController().platformChange();
    }

    @Override
    public void onChangePlatformC64(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onEditCopy(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (mainController.getVisibleEditor() != null) {
            mainController.getVisibleEditor().copy();
        }
    }

    @Override
    public void onEditCopyData(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (mainController.getVisibleEditor() != null) {
            mainController.getVisibleEditor().copyData();
        }
    }

    @Override
    public void onEditPaste(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (mainController.getVisibleEditor() != null) {
            mainController.getVisibleEditor().paste();
        }
    }

    @Override
    public void onEditSelect(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        if (mainController.getVisibleEditor() != null) {
            mainController.getVisibleEditor().select();
        }
    }

    @Override
    public void onFileOpen(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Load);
            mainController.rebuildImageSelectors();
            mainController.rebuildMapSelectors();
            mainController.rebuildTileSelectors();
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onFileQuit(ActionEvent event) {
        UIAction.quit();
    }

    @Override
    public void onFileSave(ActionEvent event) {
        ApplicationUIController mainController = ApplicationUIController.getController();
        mainController.completeInflightOperations();
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Save);
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onFileSaveAs(ActionEvent event) {
        try {
            UIAction.actionPerformed(UIAction.MAIN_ACTIONS.Save_as);
        } catch (IOException ex) {
            Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onHelpAbout(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void performUndo(ActionEvent event) {
        Editor editor = ApplicationUIController.getController().getVisibleEditor();
        if (editor != null) {
            editor.undo();
        }
    }

    @Override
    public void onToolsAuditFlags(ActionEvent event) {
        try {
            // Get game data
            var gameData = ApplicationState.getInstance().getGameData();
            if (gameData == null || gameData.getGlobal() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Game Data");
                alert.setHeaderText(null);
                alert.setContentText("Please load or create a game file first.");
                alert.showAndWait();
                return;
            }

            // Perform audit
            FlagAuditor auditor = new FlagAuditor(gameData);
            FlagAuditor.AuditReport report = auditor.performAudit();

            // Load and show dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlagAuditDialog.fxml"));
            VBox root = loader.load();

            FlagAuditDialogController controller = loader.getController();
            controller.setReport(report, auditor);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Game Flag Audit");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ApplicationState.getInstance().getPrimaryStage());
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

        } catch (Exception ex) {
            Logger.getLogger(ApplicationMenuControllerImpl.class.getName()).log(Level.SEVERE, "Error performing flag audit", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Audit Error");
            alert.setHeaderText("Failed to perform flag audit");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }
}
