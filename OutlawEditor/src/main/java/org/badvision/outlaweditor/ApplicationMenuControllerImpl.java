package org.badvision.outlaweditor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import org.badvision.outlaweditor.apple.AppleTileRenderer;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author blurry
 */
public class ApplicationMenuControllerImpl extends ApplicationMenuController {
    
    @Override
    public void onChangePlatformAppleSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        Application.currentPlatform = Platform.AppleII;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        Application.currentPlatform = Platform.AppleII;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRSolid(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = true;
        Application.currentPlatform = Platform.AppleII_DHGR;
        platformChange();
    }

    @Override
    public void onChangePlatformAppleDHGRText(ActionEvent event) {
        AppleTileRenderer.useSolidPalette = false;
        Application.currentPlatform = Platform.AppleII_DHGR;
        platformChange();
    }

    private void platformChange() {
        for (Tile t : Application.gameData.getTile()) {
            TileUtils.redrawTile(t);
        }
        ApplicationUIController mainController = ApplicationUIController.getController();
        Tile tile = mainController.getCurrentTile();
        mainController.rebuildTileSelectors();
        mainController.setCurrentTile(tile);
        if (mainController.getCurrentMapEditor() != null) {
            mainController.getCurrentMapEditor().redraw();
        }
        mainController.rebuildImageSelector();
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
            mainController.rebuildImageSelector();
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
        if (mainController.getCurrentMapEditor() != null) {
            mainController.getCurrentMapEditor().currentMap.updateBackingMap();
        }
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


}
