package org.badvision.outlaweditor.data;

import java.util.List;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Field;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.NamedEntity;
import org.badvision.outlaweditor.data.xml.Scope;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;

public class DataUtilities {

    public static void ensureGlobalExists() {
        if (Application.gameData.getGlobal() == null) {
            Application.gameData.setGlobal(new Global());
        }
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
}
