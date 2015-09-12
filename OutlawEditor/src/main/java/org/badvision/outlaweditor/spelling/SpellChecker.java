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
    private final double SIMILARITY_THRESHOLD = 0.5;

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
                Suggestion first = suggestions.stream().findFirst().get();
                if (first.similarity == 1.0) {
                    continue;
                } else {
                    SpellResponse.Source source = new SpellResponse.Source();
                    source.start = pos;
                    source.word = word;
                    response.corrections.put(source, suggestions);
                }
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
        TreeSet<Suggestion> suggestions = new TreeSet<>();
        if (word == null || word.isEmpty()) {
            return suggestions;
        }
        String lower = word.toLowerCase();
        Character first = lower.charAt(0);
        Set<String> words = dictionary.get(first);
        if (words != null) {
            if (words.contains(lower)) {
                return null;
            }
            words.parallelStream().forEach((String dictWord) -> {
                int distance = DataUtilities.levenshteinDistance(lower, dictWord);
                double similarity = distance / ((double) Math.max(lower.length(), dictWord.length()));
                if (similarity >= SIMILARITY_THRESHOLD) {
                    Suggestion suggestion = new Suggestion();
                    suggestion.similarity = similarity;
                    suggestion.word = dictWord;
                    suggestions.add(suggestion);
                }
            });
        }
        
        return suggestions;
    }

}
