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

import java.io.IOException;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.ui.ApplicationUIController;

/**
 *
 * @author brobert
 */
public class Application extends javafx.application.Application {

    public static GameData gameData = new GameData();
    public static Platform currentPlatform = Platform.AppleII;
    static Application instance;

    public static Application getInstance() {
        return instance;
    }

    private ApplicationUIController controller;

    public ApplicationUIController getController() {
        return controller;
    }

    public Stage primaryStage;

    public static Stage getPrimaryStage() {
        return instance.primaryStage;
    }

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;
        javafx.application.Platform.setImplicitExit(true);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ApplicationUI.fxml"));
        fxmlLoader.setResources(null);
        try {
            AnchorPane node = (AnchorPane) fxmlLoader.load();
            controller = fxmlLoader.getController();
            Scene s = new Scene(node);
            primaryStage.setScene(s);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.setOnCloseRequest((final WindowEvent t) -> {
            t.consume();
        });
        primaryStage.show();
    }
    Canvas tilePreview;

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
