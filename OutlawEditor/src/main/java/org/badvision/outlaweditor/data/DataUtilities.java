package org.badvision.outlaweditor.data;

import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Scripts;

public class DataUtilities {
    public static void sortScripts(Scripts s) {
        if (s == null || s.getScript() == null) {
            return;
        }
        s.getScript().sort((a, b) -> {
            if (a.getName().equalsIgnoreCase("init")) {
                return -1;
            } else if (b.getName().equalsIgnoreCase("init")) {
                return 1;
            }
            return a.getName().compareTo(b.getName());
        });
    }    

    public static void ensureGlobalExists() {
        if (Application.gameData.getGlobal() == null) {
            Application.gameData.setGlobal(new Global());
        }
    }
}
