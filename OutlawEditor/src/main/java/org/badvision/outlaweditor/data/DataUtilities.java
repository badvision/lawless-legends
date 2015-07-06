package org.badvision.outlaweditor.data;

import java.util.List;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.NamedEntity;

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
        entities.sort((a,b)->{
            String nameA = a == null ? "" : nullSafe(a.getName());
            String nameB = b == null ? "" : nullSafe(b.getName());
            return nameA.compareTo(nameB);
        });
    }
    
    public static String nullSafe(String str) {
        if (str == null) return "";
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
}
