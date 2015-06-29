package org.badvision.outlaweditor.data;

import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.data.xml.Variables;

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

    public static void sortVariables(Variables vars) {
        if (vars == null || vars.getVariable()== null) {
            return;
        }
        vars.getVariable().sort((a, b) -> {
            if (a.getName().equalsIgnoreCase("init")) {
                return -1;
            } else if (b.getName().equalsIgnoreCase("init")) {
                return 1;
            }
            return a.getName().compareTo(b.getName());
        });
    }
}
