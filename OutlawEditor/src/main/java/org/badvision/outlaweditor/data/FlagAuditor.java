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

import org.badvision.outlaweditor.data.xml.*;

import java.util.*;

/**
 * Audits game scripts to find all flag references and compare them against
 * the flags sheet. This helps identify:
 * - Flags used in scripts but not tracked in the sheet
 * - Flags in the sheet but not used in any scripts
 * - Usage patterns and locations for each flag
 *
 * @author brobert
 */
public class FlagAuditor {

    public static class FlagReference {
        public final String flagName;
        public final String scriptName;
        public final String location; // "Global" or map name
        public final String operation; // "get", "set", or "clr"

        public FlagReference(String flagName, String scriptName, String location, String operation) {
            this.flagName = flagName.toLowerCase();
            this.scriptName = scriptName;
            this.location = location;
            this.operation = operation;
        }

        @Override
        public String toString() {
            return String.format("%s: %s [%s]", location, scriptName, operation);
        }
    }

    public static class FlagInfo {
        public final String flagName;
        public final List<FlagReference> references = new ArrayList<>();
        public Integer sheetNumber = null; // null if not in sheet

        public FlagInfo(String flagName) {
            this.flagName = flagName.toLowerCase();
        }

        public int getReferenceCount() {
            return references.size();
        }

        public boolean isInSheet() {
            return sheetNumber != null;
        }

        public Set<String> getLocations() {
            Set<String> locations = new HashSet<>();
            for (FlagReference ref : references) {
                locations.add(ref.location + ": " + ref.scriptName);
            }
            return locations;
        }
    }

    public static class AuditReport {
        public final java.util.Map<String, FlagInfo> allFlags = new TreeMap<>();
        public final List<String> missingFromSheet = new ArrayList<>();
        public final List<String> unusedInSheet = new ArrayList<>();

        public FlagInfo getOrCreateFlag(String flagName) {
            return allFlags.computeIfAbsent(flagName.toLowerCase(), FlagInfo::new);
        }

        public void addReference(FlagReference ref) {
            getOrCreateFlag(ref.flagName).references.add(ref);
        }

        public void finalizeReport() {
            // Determine which flags are missing from sheet
            for (FlagInfo info : allFlags.values()) {
                if (!info.isInSheet()) {
                    missingFromSheet.add(info.flagName);
                }
            }

            // Determine which sheet flags are unused
            for (FlagInfo info : allFlags.values()) {
                if (info.isInSheet() && info.getReferenceCount() == 0) {
                    unusedInSheet.add(info.flagName);
                }
            }

            Collections.sort(missingFromSheet);
            Collections.sort(unusedInSheet);
        }
    }

    private final GameData gameData;
    private AuditReport report;

    public FlagAuditor(GameData gameData) {
        this.gameData = gameData;
    }

    public AuditReport performAudit() {
        report = new AuditReport();

        // First, scan the flags sheet to know what's tracked
        scanFlagsSheet();

        // Then scan all scripts for flag usage
        scanGlobalScripts();
        scanMapScripts();

        report.finalizeReport();
        return report;
    }

    private void scanFlagsSheet() {
        if (gameData.getGlobal() == null) return;
        Global.Sheets sheets = gameData.getGlobal().getSheets();
        if (sheets == null) return;

        for (Sheet sheet : sheets.getSheet()) {
            if (sheet.getName() != null && sheet.getName().equalsIgnoreCase("flags")) {
                Rows rows = sheet.getRows();
                if (rows != null) {
                    for (Rows.Row row : rows.getRow()) {
                        String flagName = getAttributeValue(row, "name");
                        String numberStr = getAttributeValue(row, "number");

                        if (flagName != null && !flagName.isEmpty()) {
                            FlagInfo info = report.getOrCreateFlag(flagName);
                            if (numberStr != null && !numberStr.isEmpty()) {
                                try {
                                    info.sheetNumber = Integer.parseInt(numberStr);
                                } catch (NumberFormatException e) {
                                    // Invalid number, leave as null
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    private String getAttributeValue(Rows.Row row, String attrName) {
        // Row uses dynamic attributes stored in otherAttributes map
        for (var entry : row.getOtherAttributes().entrySet()) {
            if (entry.getKey().getLocalPart().equalsIgnoreCase(attrName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void scanGlobalScripts() {
        if (gameData.getGlobal() == null) return;
        Scripts scripts = gameData.getGlobal().getScripts();
        if (scripts == null) return;

        for (Script script : scripts.getScript()) {
            scanScript(script, "Global");
        }
    }

    private void scanMapScripts() {
        if (gameData.getMap() == null) return;

        for (org.badvision.outlaweditor.data.xml.Map map : gameData.getMap()) {
            String mapName = map.getName() != null ? map.getName() : "Unnamed Map";
            Scripts scripts = map.getScripts();
            if (scripts != null) {
                for (Script script : scripts.getScript()) {
                    scanScript(script, mapName);
                }
            }
        }
    }

    private void scanScript(Script script, String location) {
        String scriptName = script.getName() != null ? script.getName() : "Unnamed Script";
        if (script.getBlock() != null) {
            scanBlock(script.getBlock(), scriptName, location);
        }
    }

    private void scanBlock(Block block, String scriptName, String location) {
        if (block == null) return;

        String blockType = block.getType();
        if (blockType != null) {
            if (blockType.equals("interaction_get_flag") ||
                blockType.equals("interaction_set_flag") ||
                blockType.equals("interaction_clr_flag")) {

                String operation = blockType.replace("interaction_", "").replace("_flag", "");
                String flagName = getFlagNameFromBlock(block);

                if (flagName != null && !flagName.isEmpty()) {
                    report.addReference(new FlagReference(flagName, scriptName, location, operation));
                }
            }
        }

        // Recursively scan nested blocks
        // Block contains a mixed list of Mutation, Field, Value, and Statement
        if (block.getMutationOrFieldOrValue() != null) {
            for (Object item : block.getMutationOrFieldOrValue()) {
                if (item instanceof Value) {
                    Value value = (Value) item;
                    if (value.getBlock() != null) {
                        for (Block nestedBlock : value.getBlock()) {
                            scanBlock(nestedBlock, scriptName, location);
                        }
                    }
                } else if (item instanceof Statement) {
                    Statement statement = (Statement) item;
                    if (statement.getBlock() != null) {
                        for (Block nestedBlock : statement.getBlock()) {
                            scanBlock(nestedBlock, scriptName, location);
                        }
                    }
                }
                // Field and Mutation don't contain blocks, so we skip them
            }
        }

        // Follow the "next" chain
        if (block.getNext() != null && block.getNext().getBlock() != null) {
            scanBlock(block.getNext().getBlock(), scriptName, location);
        }
    }

    private String getFlagNameFromBlock(Block block) {
        if (block.getMutationOrFieldOrValue() == null) return null;

        for (Object item : block.getMutationOrFieldOrValue()) {
            if (item instanceof Field) {
                Field field = (Field) item;
                if ("NAME".equals(field.getName())) {
                    return field.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Adds missing flags to the flags sheet with auto-assigned numbers
     */
    public void appendMissingFlagsToSheet(AuditReport report) {
        if (report.missingFromSheet.isEmpty()) return;

        // Find or create the flags sheet
        Sheet flagsSheet = findOrCreateFlagsSheet();

        // Find the highest existing flag number
        int maxNumber = 0;
        for (Rows.Row row : flagsSheet.getRows().getRow()) {
            String numberStr = getAttributeValue(row, "number");
            if (numberStr != null && !numberStr.isEmpty()) {
                try {
                    int num = Integer.parseInt(numberStr);
                    maxNumber = Math.max(maxNumber, num);
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers
                }
            }
        }

        // Add missing flags
        for (String flagName : report.missingFromSheet) {
            maxNumber++;
            Rows.Row newRow = new Rows.Row();

            // Set attributes using the dynamic attributes map
            newRow.getOtherAttributes().put(new javax.xml.namespace.QName("name"), flagName);
            newRow.getOtherAttributes().put(new javax.xml.namespace.QName("number"), String.valueOf(maxNumber));

            flagsSheet.getRows().getRow().add(newRow);
        }
    }

    private Sheet findOrCreateFlagsSheet() {
        Global global = gameData.getGlobal();
        if (global == null) {
            global = new Global();
            gameData.setGlobal(global);
        }

        Global.Sheets sheets = global.getSheets();
        if (sheets == null) {
            sheets = new Global.Sheets();
            global.setSheets(sheets);
        }

        // Look for existing flags sheet
        for (Sheet sheet : sheets.getSheet()) {
            if (sheet.getName() != null && sheet.getName().equalsIgnoreCase("flags")) {
                return sheet;
            }
        }

        // Create new flags sheet
        Sheet flagsSheet = new Sheet();
        flagsSheet.setName("flags");

        Columns columns = new Columns();
        UserType nameCol = new UserType();
        nameCol.setName("name");
        UserType numberCol = new UserType();
        numberCol.setName("number");
        columns.getColumn().add(nameCol);
        columns.getColumn().add(numberCol);
        flagsSheet.setColumns(columns);

        Rows rows = new Rows();
        flagsSheet.setRows(rows);

        sheets.getSheet().add(flagsSheet);
        return flagsSheet;
    }
}
