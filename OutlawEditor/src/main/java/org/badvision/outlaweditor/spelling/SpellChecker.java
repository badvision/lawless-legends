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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.badvision.outlaweditor.data.DataUtilities;

/**
 *
 * @author blurry
 */
public class SpellChecker {
    private static HashMap<Character, Set<String>> dictionary;

    public SpellChecker() {
        loadDictionary();
    }

    public SpellResponse check(String value) {
        SpellResponse response = new SpellResponse();
        String[] words = value.split("[^A-Za-z]");
        int pos = 0;
        for (String word : words) {
            Set<Suggestion> suggestions = getSuggestions(word);
            if (suggestions != null && !suggestions.isEmpty()) {
                SpellResponse.Source source = new SpellResponse.Source();
                source.start = pos;
                source.word = word;
                response.corrections.put(source, suggestions);
            }

            pos += word.length() + 1;
        }
        return response;
    }

    private static void loadDictionary() {
        if (dictionary == null) {
            URL dictionaryPath = SpellChecker.class.getResource("/mythos/dictionary.txt");
            try {
                BufferedReader content = new BufferedReader(new InputStreamReader((InputStream) dictionaryPath.getContent()));
                dictionary = new HashMap<>();
                content.lines().forEach((String word)-> {
                    String lower = word.toLowerCase();
                    Set<String> words = dictionary.get(lower.charAt(0));
                    if (words == null) {
                        words = new LinkedHashSet<>();
                        dictionary.put(lower.charAt(0), words);
                    }
                    words.add(word);
                });
            } catch (IOException ex) {
                Logger.getLogger(SpellChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }

    private Set<Suggestion> getSuggestions(String word) {
        Set<Suggestion> suggestions = Collections.synchronizedSet(new TreeSet<>());
        if (word == null || word.isEmpty()) {
            return suggestions;
        }
        String lower = word.toLowerCase();
        Character first = lower.charAt(0);
        Set<String> words = dictionary.get(first);
        int length = lower.length();
        double threshold = length <= 2 ? 0 : Math.log(length-1) * 1.75;
        if (words != null) {
            if (lower.length() <= 2 || words.contains(lower)) {
                return null;
            }
            words.parallelStream().forEach((String dictWord) -> {
                int distance = DataUtilities.levenshteinDistance(lower, dictWord, (int) threshold);
                if (distance <= threshold) {
                    Suggestion suggestion = new Suggestion();
                    suggestion.original = lower;
                    suggestion.similarity = distance;
                    suggestion.word = dictWord;
                    suggestions.add(suggestion);
                }
            });
            if (suggestions.isEmpty()) {
                Suggestion suggestion = new Suggestion();
                suggestion.original = lower;
                suggestion.similarity = 100;
                suggestion.word = "????";
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }

}
