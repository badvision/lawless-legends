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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.xml.Block;
import org.badvision.outlaweditor.data.xml.Field;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.NamedEntity;
import org.badvision.outlaweditor.data.xml.Scope;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.data.xml.Statement;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.data.xml.Value;
import org.badvision.outlaweditor.ui.UIAction;

public class DataUtilities {

    public static void logDataStructure(GameData gameData) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintWriter logger = new PrintWriter(os);
            if (gameData != null) {
                logger.println("Game data detected");
                logMapStructure(gameData.getMap(), logger);
                logGlobalStrcture(gameData.getGlobal(), logger);
                logImageStructure(gameData.getImage(), logger);
                logTileStructure(gameData.getTile(), logger);
            } else {
                logger.println("Game data was not detected");
            }
            logger.flush();
            System.out.print(os.toString());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5sum = digest.digest(os.toByteArray());
            System.out.println();
            System.out.print("checksum: ");
            for (int i = 0; i < md5sum.length; i++) {
                System.out.printf("%02X", md5sum[i]);
            }
            System.out.println();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(DataUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void logGlobalStrcture(Global global, PrintWriter logger) {
        if (global != null) {
            logScripts(global.getScripts(), logger);
            
        } else {
            logger.println("No global data was detected");
        }
    }
    
    private static void logScripts(Scripts scripts, PrintWriter logger) {
        if (scripts != null && scripts.getScript() != null && !scripts.getScript().isEmpty()) {
            scripts.getScript().forEach((script) -> {
                if (script.getBlock() == null) {
                    return;
                }
                Queue<Block> evaluateStack = new ArrayDeque<>();
                evaluateStack.add(script.getBlock());
                int blockCount = 0;
                while (!evaluateStack.isEmpty()) {
                    Block current = evaluateStack.poll();
                    blockCount++;
                    if (current.getNext() != null && current.getNext().getBlock() != null) {
                        evaluateStack.add(current.getNext().getBlock());
                    }
                    extract(current, Value.class).map(Value::getBlock).filter(Objects::nonNull).forEach(evaluateStack::addAll);
                    extract(current, Statement.class).map(Statement::getBlock).filter(Objects::nonNull).forEach(evaluateStack::addAll);
                }
                logger.println("Script " + script.getName() + "; " + blockCount + " blocks");
            });
        } else {
            logger.println("No scripts were detected");            
        }
    }

    private static void logImageStructure(List<Image> images, PrintWriter logger) {
        if (images != null && images.size() > 0) {
            logger.println(images.size() + " images were detected");            
        } else {
            logger.println("No images were detected");
        }
    }

    private static void logMapStructure(List<Map> maps, PrintWriter logger) {
        if (maps != null && maps.size() > 0) {
            for (Map m : maps) {
                logger.println(">> Map "+m.getName());
                logScripts(m.getScripts(), logger);
            }
        } else {
            logger.println("No maps were detected");
        }
        
    }    
    
    private static void logTileStructure(List<Tile> tiles, PrintWriter logger) {
        if (tiles != null && tiles.size() > 0) {
            logger.println(tiles.size() + " tiles were detected");            
        } else {
            logger.println("No tiles were detected");
        }
    }    
    
    private DataUtilities() {
    }

    public static void ensureGlobalExists() {
        if (ApplicationState.getInstance().getGameData().getGlobal() == null) {
            ApplicationState.getInstance().getGameData().setGlobal(new Global());
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
        extract(script.getBlock(), Field.class)
                .filter((f) -> f.getName().equalsIgnoreCase("NAME"))
                .findFirst().ifPresent(
                        (f) -> script.setName(f.getValue())
                );
    }

    public static void cleanupScriptNames(Scope s) {
        if (s.getScripts() == null || s.getScripts().getScript() == null) {
            return;
        }
        s.getScripts().getScript().forEach(DataUtilities::cleanupScriptName);
    }

    public static void cleanupAllScriptNames() {
        cleanupScriptNames(ApplicationState.getInstance().getGameData().getGlobal());
        ApplicationState.getInstance().getGameData().getMap().forEach(DataUtilities::cleanupScriptNames);
    }

    public static <T> Optional<T> extractFirst(Block block, Class<T> desiredType) {
        return extract(block, desiredType).findFirst();
    }

    public static <T> Stream<T> extract(Block block, Class<T> desiredType) {
        if (block != null && block.getMutationOrFieldOrValue() != null) {
            return (Stream<T>) block.getMutationOrFieldOrValue().stream().filter((o) -> o.getClass().equals(desiredType));
        } else {
            return Stream.empty();
        }
    }

    public static String getValue(java.util.Map<QName, String> map, String name) {
        return map.entrySet().stream()
                .filter((e) -> e.getKey().getLocalPart().equals(name))
                .map(e -> e.getValue())
                .findFirst().orElse(null);

    }

    public static void setValue(java.util.Map<QName, String> map, String name, String newValue) {
        Optional<java.util.Map.Entry<QName, String>> attr = map.entrySet().stream()
                .filter((e) -> e.getKey().getLocalPart().equals(name)).findFirst();
        if (attr.isPresent()) {
            attr.get().setValue(newValue);
        } else {
            map.put(new QName(name), newValue);
        }
    }

    public static List<List<String>> readFromFile(File file) {
        try {
            if (file.getName().toLowerCase().endsWith("txt")
                    || file.getName().toLowerCase().endsWith("tsv")) {
                return readTextFile(file);
            } else if (file.getName().toLowerCase().endsWith("xls")) {
                return readLegacyExcel(file);
            } else if (file.getName().toLowerCase().endsWith("xlsx")) {
                return readExcel(file);
            }
        } catch (IOException | InvalidFormatException ex) {
            Logger.getLogger(DataUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        UIAction.alert("Couldn't figure out how to import file " + file.getName());
        return Collections.EMPTY_LIST;
    }

    public static List<List<String>> readTextFile(File file) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        return reader.lines().map(line -> Arrays.asList(line.split("\\t"))).collect(Collectors.toList());
    }

    public static List<List<String>> readLegacyExcel(File file) throws FileNotFoundException, IOException {
        return readSheet(new HSSFWorkbook(new FileInputStream(file)));
    }

    public static List<List<String>> readExcel(File file) throws FileNotFoundException, IOException, InvalidFormatException {
        return readSheet(new XSSFWorkbook(file));
    }

    public static List<List<String>> readSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        List<List<String>> data = new ArrayList<>();
        sheet.forEach(row -> {
            List<String> rowData = new ArrayList<>();
            row.forEach(cell -> {
                String col = getStringValueFromCell(cell);
                rowData.add(col);
            });
            data.add(rowData);
        });
        return data;
    }

    public static String getStringValueFromCell(Cell cell) {
        switch (cell.getCellType()) {
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case BLANK:
                return null;
            case NUMERIC:
                return Double.toString(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            default:
                return "???";
        }
    }

    public static String hexDump(byte[] data) {
        StringBuilder dump = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                dump.append(",");
            }
            dump.append(getHexValueFromByte(data[i]));
        }
        return dump.toString();
    }

    public static String getHexValueFromByte(byte val) {
        return getHexValue(val & 0x0ff);
    }

    public static String getHexValue(int val) {
        if (val < 16) {
            return "0" + Integer.toHexString(val);
        } else {
            return Integer.toHexString(val);
        }
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
            double s1 = levenshteinDistance(match, o1, 20);
            double s2 = levenshteinDistance(match, o2, 20);
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
