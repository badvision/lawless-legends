package org.badvision.outlaweditor.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.Event;
import javafx.fxml.FXML;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.Editor;

public abstract class ApplicationUIController {

    public static ApplicationUIController getController() {
        return Application.getInstance().getController();
    }

    abstract public void rebuildTileSelectors();

    abstract public void rebuildMapSelectors();

    abstract public void rebuildImageSelectors();

    public abstract Editor getVisibleEditor();

    @FXML // ResourceBundle that was given to the FXMLLoader
    protected ResourceBundle resources;
    @FXML
    protected ApplicationMenuController menuController;
    @FXML
    protected TileEditorTabController tileController;
    @FXML
    protected MapEditorTabController mapController;
    @FXML
    protected ImageEditorTabController imageController;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    protected URL location;

    abstract public void platformChange();

    @FXML
    abstract public void tileTabActivated(Event event);

    @FXML
    abstract public void mapTabActivated(Event event);

    @FXML
    abstract public void imageTabActivated(Event event);

    @FXML
    abstract public void scriptTabActivated(Event event);

    @FXML // This method is called by the FXMLLoader when initialization is complete
    public void initialize() {
    }

    abstract public void completeInflightOperations();

    abstract public void clearData();

    abstract public void updateSelectors();

    abstract public void redrawScripts();
}
