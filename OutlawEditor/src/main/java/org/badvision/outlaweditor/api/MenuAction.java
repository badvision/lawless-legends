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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Any ser
 * @author blurry
 */
public interface MenuAction extends EventHandler<ActionEvent> {
    public String getName();
    
    default public String getDescription() {
        return getName();
    }
}
