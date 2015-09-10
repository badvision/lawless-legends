/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import org.badvision.outlaweditor.data.PropertyHelper;
import org.badvision.outlaweditor.ui.impl.ApplicationUIControllerImpl;

/**
 *
 * @author blurry
 */
public abstract class EntitySelectorCell<T> extends ComboBoxListCell<T> {

    static Map<TextField, Object> lastSelected = new HashMap<>();
    TextField nameField, categoryField;

    public EntitySelectorCell(TextField tileNameField, TextField categoryNameField) {
        super.setPrefWidth(125);
        nameField = tileNameField;
        categoryField = categoryNameField;
    }

    @Override
    public void updateSelected(boolean sel) {
        updateItem(getItem(), false);
    }

    @Override
    public void updateItem(T item, boolean empty) {
        textProperty().unbind();
        super.updateItem(item, empty);
        if (item != null && !(item instanceof String)) {
            try {
                if (categoryField != null) {
                    textProperty().bind(
                            Bindings.concat(
                                    PropertyHelper.stringProp(item, "category"),
                                    "/",
                                    PropertyHelper.stringProp(item, "name")
                            )
                    );
                } else {
                    textProperty().bind(PropertyHelper.stringProp(item, "name"));
                }
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ApplicationUIControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            finishUpdate(item);
        } else {
            setText(null);
        }
    }

    public void finishUpdate(T item) {
    }

}
