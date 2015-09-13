/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor.spelling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SpellResponse {
    public static class Source {
        public int start;
        public String word;
    }
    
    Map<Source, Set<Suggestion>> corrections = new LinkedHashMap<>();
    public int getErrors() {
        return corrections.size();
    }
    
    public Map<Source, Set<Suggestion>> getCorrections() {
        return corrections;
    }
}
