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
package org.badvision.outlaweditor.ui.impl;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import org.badvision.outlaweditor.SheetEditor;
import org.badvision.outlaweditor.ui.SheetEditorController;
public class SheetEditorControllerImpl extends SheetEditorController {

    private SheetEditor editor;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize();
    }    

    public void setEditor(SheetEditor editor) {
        this.editor = editor;
//        editor.getSheet().
    }

    @Override
    public void addColumnAction(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
