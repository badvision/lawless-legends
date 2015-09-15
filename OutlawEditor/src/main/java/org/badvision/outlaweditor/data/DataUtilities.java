/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Field;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.NamedEntity;
import org.badvision.outlaweditor.data.xml.Scope;
import org.badvision.outlaweditor.data.xml.Script;

public class DataUtilities {
    private DataUtilities() {
    }

    public static void ensureGlobalExists() {
        if (Application.gameData.getGlobal() == null) {
            Application.gameData.setGlobal(new Global());
        }
    }

    public static void sortMaps(List<? extends Map> entities) {
        if (entities == null) {
            return;
        }
        entities.sort((a, b) -> {
            String nameA = a == null ? "" : nullSafe(a.getName());
            String nameB = b == null ? "" : nullSafe(b.getName());
            if (nameA.equalsIgnoreCase("init")) {
                return -1;
            }
            if (nameB.equalsIgnoreCase("init")) {
                return 1;
            }
            return nameA.compareTo(nameB);
        });
    }
    
    public static void sortNamedEntities(List<? extends NamedEntity> entities) {
        if (entities == null) {
            return;
        }
        entities.sort((a, b) -> {
            String nameA = a == null ? "" : nullSafe(a.getName());
            String nameB = b == null ? "" : nullSafe(b.getName());
            if (nameA.equalsIgnoreCase("init")) {
                return -1;
            }
            if (nameB.equalsIgnoreCase("init")) {
                return 1;
            }
            return nameA.compareTo(nameB);
        });
    }

    public static String nullSafe(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    public static String uppercaseFirst(String str) {
        StringBuilder b = new StringBuilder(str);
        int i = 0;
        do {
            b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
            i = b.indexOf(" ", i) + 1;
        } while (i > 0 && i < b.length());
        return b.toString();
    }

    public static void cleanupScriptName(Script script) {
        if (script.getName() != null) {
            return;
        }
        script.getBlock().getFieldOrMutationOrStatement().stream()
                .filter((obj) -> (obj instanceof Field && ((Field) obj).getName().equalsIgnoreCase("NAME")))
                .forEach((obj) -> {
                    script.setName(((Field) obj).getValue());
                });
    }
    
    public static void cleanupScriptNames(Scope s) {
        if (s.getScripts() == null || s.getScripts().getScript() == null) return;
        s.getScripts().getScript().forEach(DataUtilities::cleanupScriptName);
    }
    
    public static void cleanupAllScriptNames() {
        cleanupScriptNames(Application.gameData.getGlobal());
        Application.gameData.getMap().forEach(DataUtilities::cleanupScriptNames);
    }

    //------------------------------ String comparators
    /**
     * Rank two strings similarity in terms of distance The lower the number,
     * the more similar these strings are to each other See:
     * http://en.wikipedia.org/wiki/Levenshtein_distance#Computing_Levenshtein_distance
     *
     * @param s
     * @param t
     * @param limit
     * @return Distance (higher is better)
     */
    public static int levenshteinDistance(String s, String t, int limit) {
        int sizeDiff = Math.abs(s.length() - t.length());
        if (sizeDiff > limit) {
            return sizeDiff;
        }

        s = s.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        t = t.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        int m = s.length();
        int n = t.length();
        int[][] dist = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            dist[i][0] = i;
        }
        for (int i = 1; i <= n; i++) {
            dist[0][i] = i;
        }
        for (int j = 1; j <= n; j++) {
            int min = 100;
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dist[i][j] = dist[i - 1][j - 1];
                } else {
                    int del = dist[i - 1][j] + 1;
                    int insert = dist[i][j - 1] + 1;
                    int sub = dist[i - 1][j - 1] + 1;
                    dist[i][j] = Math.min(Math.min(del, insert), sub);
                }
                min = Math.min(min, dist[i][j]);
            }
            if (min > limit) {
                return min;
            }
        }
        return dist[m][n];
    }

    /**
     * Compare strings based on a tally of similar patterns found, using a fixed
     * search window The resulting score is heavily penalized if the strings
     * differ greatly in length This is not as efficient as levenshtein, so it's
     * only used as a tie-breaker.
     *
     * @param c1
     * @param c2
     * @param width Search window size
     * @return Overall similarity score (higher is better)
     */
    public static double rankMatch(String c1, String c2, int width) {
        double score = 0;
        String s1 = c1.toLowerCase();
        String s2 = c2.toLowerCase();
        for (int i = 0; i < s1.length() + 1 - width; i++) {
            String m = s1.substring(i, i + width);
            int j = 0;
            while ((j = s2.indexOf(m, j)) > -1) {
                score += width;
                j++;
            }
        }
        double l1 = s1.length();
        double l2 = s2.length();
        // If the two strings are equivilent in length, the score is higher
        // If the two strings are different in length, the score is adjusted lower depending on how large the difference is
        // This is offset just a hair for tuning purposes
        double adjustment = (Math.min(l1, l2) / Math.max(l1, l2)) + 0.1;
        return score * adjustment * adjustment;
    }

    public static class RankingComparator implements Comparator<String> {

        String match;

        public RankingComparator(String match) {
            // Adding a space helps respect word boundaries as part of the match
            // In the case of very close matches this is another tie-breaker
            // Especially for very small search terms
            this.match = match + " ";
        }

        @Override
        public int compare(String o1, String o2) {
            double s1 = levenshteinDistance(match, o1,20);
            double s2 = levenshteinDistance(match, o2,20);
            if (s2 == s1) {
                s1 = rankMatch(o1, match, 3) + rankMatch(o1, match, 2);
                s2 = rankMatch(o2, match, 3) + rankMatch(o2, match, 2);
                if (s2 == s1) {
                    return (o1.compareTo(o2));
                } else {
                    // Normalize result to -1, 0 or 1 so there is no rounding issues!
                    return (int) Math.signum(s2 - s1);
                }
            } else {
                return (int) (s2 - s1);
            }
        }
    }

    /**
     * Given a desired search string and a search space of recognized
     * selections, identify the best match in the list
     *
     * @param match String to search for
     * @param search Space of all valid results
     * @return Best match found, or null if there was nothing close to a match
     * found.
     */
    public static String findBestMatch(String match, Collection<String> search) {
        if (search == null || search.isEmpty()) {
            return null;
        }
        RankingComparator r = new RankingComparator(match);
        List<String> candidates = new ArrayList<>(search);
        Collections.sort(candidates, r);
        double score = levenshteinDistance(match, candidates.get(0), 20);
        if (score > 1) {
            return candidates.get(0);
        }
        return null;
    }
}
