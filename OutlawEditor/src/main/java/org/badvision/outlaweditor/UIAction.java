/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXB;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.GameData;

/**
 *
 * @author brobert
 */
public class UIAction {

    private static File currentSaveFile;

    public static enum MAIN_ACTIONS {

        _General,
        Load, Save, Save_as, Apple_Mode, C64_Mode, About_Outlaw_Editor, Quit,
        _Map,
        Create_new_map, Export_map_to_image,
        _Tiles,
        Create_new_tile, Export_tile_to_image, Delete_tile,
        _Images,
        Create_new_image, Import_image, Export_image_as_raw, Delete_image;
    };

    public static void actionPerformed(MAIN_ACTIONS action) throws IOException {
        switch (action) {
            case About_Outlaw_Editor:
                break;
            case Apple_Mode:
                break;
            case C64_Mode:
                break;
            case Delete_tile:
//                if (currentSelectedTile != null) {
//                    Tile t = currentSelectedTile;
//                    selectTile(null, null);
//                    TilesetUtils.remove(t);
//                    Application.instance.redrawTileSelector();
//                    Application.instance.rebuildMapEditor();
//                }
                break;
            case Export_map_to_image:
                break;
            case Export_tile_to_image:
                break;
            case Load:
                File f = FileUtils.getFile(currentSaveFile, "Load game data", Boolean.FALSE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                if (f == null) {
                    return;
                }
                currentSaveFile = f;
                GameData newData = JAXB.unmarshal(currentSaveFile, GameData.class);
                Application.instance.controller.setCurrentMap(null);
                Application.instance.controller.setCurrentTile(null);
                TilesetUtils.clear();
                Application.gameData = newData;
                Application.instance.controller.rebuildTileSelectors();
                Application.instance.controller.rebuildMapSelectors();
                break;
            case Quit:
                quit();
                break;
            case Save_as:
                f = FileUtils.getFile(currentSaveFile, "Save game data", Boolean.TRUE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                if (f == null) {
                    return;
                }
                currentSaveFile = f;
            case Save:
                if (currentSaveFile == null) {
                    currentSaveFile = FileUtils.getFile(currentSaveFile, "Save game data", Boolean.TRUE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                }
                if (currentSaveFile != null) {
                    currentSaveFile.delete();
                    JAXB.marshal(Application.gameData, currentSaveFile);
                }
                break;
        }
    }

    public static MenuBar buildMenu() {
        MenuBar menu = new MenuBar();
        Menu currentMenu = null;
        for (final MAIN_ACTIONS action : UIAction.MAIN_ACTIONS.values()) {
            if (action.name().startsWith("_")) {
                if (currentMenu != null) {
                    menu.getMenus().add(currentMenu);
                }
                currentMenu = new Menu(action.name().replace("_", ""));
            } else {
                MenuItem item = new MenuItem(action.name().replaceAll("_", " "));
                item.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent t) {
                        try {
                            actionPerformed(action);
                        } catch (IOException ex) {
                            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                currentMenu.getItems().add(item);
            }
        }
        menu.getMenus().add(currentMenu);
        menu.setMinWidth(1.0);
        return menu;
    }

    public static boolean quit() {
        if (JOptionPane.showConfirmDialog(null, "Quit?  Are you sure?") == JOptionPane.OK_OPTION) {
            Platform.exit();
            return true;
        }
        return false;
    }

    static Image badImage;

    public static WritableImage getBadImage(int width, int height) {
        if (badImage == null) {
            badImage = new Image("/org/badvision/outlaw/resources/icon_brokenLink.png");
        }
        WritableImage img = new WritableImage(width, height);
        img.getPixelWriter().setPixels(0, 0, (int) badImage.getWidth(), (int) badImage.getHeight(), badImage.getPixelReader(), 0, 0);

        return img;
    }
    
    public static boolean confirm(String string) {
        return JOptionPane.showConfirmDialog(null, string) == JOptionPane.YES_OPTION;
    }    
}