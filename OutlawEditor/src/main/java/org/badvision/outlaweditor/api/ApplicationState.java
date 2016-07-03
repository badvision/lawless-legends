/*
 * Copyright 2016 org.badvision.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.badvision.outlaweditor.api;

import javafx.stage.Stage;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author blurry
 */
public interface ApplicationState {
    public GameData getGameData();

    public void setGameData(GameData newData);

    public ApplicationUIController getApplicationUI();

    public Platform getCurrentPlatform();

    public void setCurrentPlatform(Platform p);

    public ApplicationUIController getController();

    public Stage getPrimaryStage();

    static public BundleContext getBundleContext() {
        if (Application.felix != null) {
            return Application.felix.getBundleContext();
        } else {
            return FrameworkUtil.getBundle(ApplicationState.class).getBundleContext();
        }        
    }
    
    public static ApplicationState getInstance() {
        BundleContext bc = getBundleContext();
        return bc.getService(bc.getServiceReference(ApplicationState.class));
    }

}
