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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.impl.Activator;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.api.MenuAction;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author brobert
 */
@Component
@Service(org.badvision.outlaweditor.api.ApplicationState.class)
public class Application extends javafx.application.Application implements ApplicationState, BundleActivator {

    public static Framework felix;
    private GameData gameData = new GameData();
    private Platform currentPlatform = Platform.AppleII;
    private ApplicationUIController controller;
    private Stage primaryStage;

    public static void shutdown() {
        try {
            felix.stop();
            felix.waitForStop(0L);
        } catch (BundleException | InterruptedException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public GameData getGameData() {
        return gameData;
    }

    @Override
    public ApplicationUIController getApplicationUI() {
        return controller;
    }

    @Override
    public Platform getCurrentPlatform() {
        return currentPlatform;
    }

    @Override
    public void setGameData(GameData newData) {
        gameData = newData;
    }

    @Override
    public void setCurrentPlatform(Platform newPlatform) {
        currentPlatform = newPlatform;
    }

    @Override
    public ApplicationUIController getController() {
        return controller;
    }

    @Override
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        javafx.application.Platform.setImplicitExit(true);

        try {
            startPluginContainer();
        } catch (Exception ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        
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

    ServiceTracker tracker;
    private void startPluginContainer() throws BundleException, Exception {
        Map<String, String> config = new HashMap<>();
        config.put(FelixConstants.ACTIVATION_LAZY, "false");
        config.put("felix.cache.locking", "false");
        config.put("felix.auto.deploy.dir", "install");
        config.put("felix.auto.deploy.action", "install,start");
        config.put("org.osgi.framework.system.packages.extra",
                "javafx.event,"
                + "org.badvision.outlaweditor.api,"
                + "org.badvision.outlaweditor.data,"
                + "org.badvision.outlaweditor.data.xml,"
                + "org.badvision.outlaweditor.ui,"
                + "org.osgi.framework");
        // MH: Had to add these to allow plugin to access basic Java XML classes, 
        //     and other stuff you'd think would just be default.
        config.put("org.osgi.framework.bootdelegation", 
                "sun.*,"
                + "com.sun.*,"
                + "org.w3c.*,"
                + "org.xml.*,"
                + "javax.xml.*");
        felix = new Felix(config);
        felix.start();
        felix.getBundleContext().registerService(ApplicationState.class, this, null);
        tracker = new ServiceTracker(felix.getBundleContext(), MenuAction.class, null);
        tracker.open();
        Activator scrActivator = new Activator();
        scrActivator.start(felix.getBundleContext());
        AutoProcessor.process(config, felix.getBundleContext());
    }
    
    @Override
    public void start(BundleContext bc) throws Exception {
        launch();
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
    }
}
