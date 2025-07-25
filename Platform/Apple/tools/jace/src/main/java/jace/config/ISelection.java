/** 
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.config;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */
public interface ISelection<T> extends Serializable {

    LinkedHashMap<? extends T, String> getSelections();

    T getValue();

    void setValue(T value);
    
    void setValueByMatch(String value);
}
