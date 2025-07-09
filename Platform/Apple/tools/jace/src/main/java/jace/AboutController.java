package jace;

import jace.core.VersionInfo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

/**
 * Controller for the About window
 * 
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class AboutController implements Initializable {

    @FXML
    private Button closeButton;
    
    @FXML
    private ImageView appIcon;
    
    @FXML
    private Label versionLabel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load application icon
        try {
            String iconPath = "/jace/data/game_icon.png";
            InputStream iconStream = getClass().getResourceAsStream(iconPath);
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                appIcon.setImage(icon);
            }
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }
        
        // Set version information
        if (versionLabel != null) {
            versionLabel.setText(VersionInfo.getVersionDisplay());
        }
    }
    
    /**
     * Close the About window when the close button is clicked
     */
    @FXML
    private void handleCloseButton() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
} 