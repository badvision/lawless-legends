/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.DataUtilities;
import org.badvision.outlaweditor.data.xml.Field;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.data.xml.Statement;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.MythosScriptEditorController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javafx.event.Event;
import javafx.stage.Stage;

/**
 * After Apply, the model must agree with the committed Blockly XML: the script name is
 * synced from the root block's NAME field, the map keeps referencing the exact same
 * Script instance (no clones/replacements), and unrelated model state (location
 * triggers) survives untouched.
 */
public class MythosEditorApplyNameTest {

    // Blockly-style workspace XML as produced by the editor: a <variables> prefix
    // followed by the root "procedures_defreturn" block carrying the NAME field.
    private static final String RENAMED_XML =
        "<variables></variables>"
            + "<block type=\"procedures_defreturn\" id=\"x\" inline=\"false\" x=\"5\" y=\"5\">"
            + "<mutation></mutation>"
            + "<field name=\"NAME\">Saloon_Renamed</field>"
            + "</block>";

    private static final String NO_NAME_FIELD_XML =
        "<block type=\"procedures_defreturn\" id=\"y\" inline=\"false\" x=\"5\" y=\"5\">"
            + "<mutation></mutation>"
            + "</block>";

    private static final String EMPTY_NAME_FIELD_XML =
        "<block type=\"procedures_defreturn\" id=\"z\" inline=\"false\" x=\"5\" y=\"5\">"
            + "<mutation></mutation>"
            + "<field name=\"NAME\"></field>"
            + "</block>";

    private StubState state;

    @Before
    public void installAppState() {
        state = new StubState();
        Application.applicationStateSingleton = state;
    }

    @After
    public void clearAppState() {
        Application.applicationStateSingleton = null;
    }

    private static MythosScriptEditorController xmlController(final String xml) {
        return new MythosScriptEditorController() {
            @Override
            public String getScriptXml() {
                return xml;
            }
        };
    }

    private Script scriptInMap(Map map, String name) {
        Script script = new Script();
        script.setName(name);
        Script.LocationTrigger trigger = new Script.LocationTrigger();
        trigger.setX(3);
        trigger.setY(7);
        script.getLocationTrigger().add(trigger);
        map.setScripts(new Scripts());
        map.getScripts().getScript().add(script);
        return script;
    }

    @Test
    public void applyChangesSyncsNameFromRootBlockXml() {
        Map map = new Map();
        Script script = scriptInMap(map, "Saloon");
        Script.LocationTrigger trigger = script.getLocationTrigger().get(0);

        MythosEditor editor = new MythosEditor(script, null);
        editor.controller = xmlController(RENAMED_XML);
        editor.applyChanges();

        assertEquals("Apply must sync the script name from the root block's NAME field",
            "Saloon_Renamed", script.getName());
        assertTrue("the map must still reference the same Script instance",
            map.getScripts().getScript().contains(script));
        assertSame(script, map.getScripts().getScript().get(0));
        assertEquals(1, script.getLocationTrigger().size());
        assertSame("location triggers must survive Apply untouched", trigger, script.getLocationTrigger().get(0));
        assertNotNull("Apply must commit the block from the XML", script.getBlock());
        assertEquals("procedures_defreturn", script.getBlock().getType());
        assertEquals("the map script lists must be redrawn when the name changes",
            1, state.controller.redraws);
    }

    @Test
    public void applyChangesUsesRootNameNotNestedFunctionName() {
        String xml = "<variables></variables>"
            + "<block type=\"procedures_defreturn\" id=\"root\">"
            + "<mutation></mutation>"
            + "<statement name=\"DO\"><block type=\"procedures_defreturn\" id=\"nested\">"
            + "<field name=\"NAME\">NestedFn</field></block></statement>"
            + "<field name=\"NAME\">RootName</field>"
            + "</block>";
        Map map = new Map();
        Script script = scriptInMap(map, "Original");
        Script.LocationTrigger trigger = script.getLocationTrigger().get(0);

        MythosEditor editor = new MythosEditor(script, null);
        editor.controller = xmlController(xml);
        editor.applyChanges();

        assertEquals("only the root block's NAME may name the script", "RootName", script.getName());
        assertSame("Apply must preserve the map-to-script reference", script, map.getScripts().getScript().get(0));
        assertEquals(1, script.getLocationTrigger().size());
        assertSame("Apply must preserve the location trigger", trigger, script.getLocationTrigger().get(0));
        assertNotNull("Apply must commit the root block", script.getBlock());
        Statement statement = DataUtilities.extractFirst(script.getBlock(), Statement.class).orElseThrow();
        Field nestedName = DataUtilities.extractFirst(statement.getBlock().get(0), Field.class).orElseThrow();
        assertEquals("the nested definition must actually be present in the committed XML", "NestedFn", nestedName.getValue());
        assertEquals(1, state.controller.redraws);
    }

    @Test
    public void applyChangesWithoutNameFieldLeavesNameUntouched() {
        Map map = new Map();
        Script script = scriptInMap(map, "Saloon");

        MythosEditor editor = new MythosEditor(script, null);
        editor.controller = xmlController(NO_NAME_FIELD_XML);
        editor.applyChanges();

        assertEquals("no NAME field in the XML: the model name must be left untouched",
            "Saloon", script.getName());
        assertSame("the map must still reference the same Script instance",
            script, map.getScripts().getScript().get(0));
        assertEquals("no rename happened, so no redraw is needed", 0, state.controller.redraws);
    }

    @Test
    public void applyChangesWithEmptyNameFieldLeavesNameUntouched() {
        Map map = new Map();
        Script script = scriptInMap(map, "Saloon");

        MythosEditor editor = new MythosEditor(script, null);
        editor.controller = xmlController(EMPTY_NAME_FIELD_XML);
        editor.applyChanges();

        assertEquals("an empty NAME field must not blank out the model name",
            "Saloon", script.getName());
        assertSame(script, map.getScripts().getScript().get(0));
        assertEquals(0, state.controller.redraws);
    }

    // ---------------------------------------------------------------------
    // Test doubles for the application-wide state that setFunctionName()
    // touches through ApplicationUIController.getController().
    // ---------------------------------------------------------------------

    private static final class StubUIController extends ApplicationUIController {
        int redraws = 0;

        @Override
        public void rebuildTileSelectors() {
        }

        @Override
        public void rebuildMapSelectors() {
        }

        @Override
        public void rebuildImageSelectors() {
        }

        @Override
        public Editor getVisibleEditor() {
            return null;
        }

        @Override
        public void platformChange() {
        }

        @Override
        public void tileTabActivated(Event event) {
        }

        @Override
        public void mapTabActivated(Event event) {
        }

        @Override
        public void imageTabActivated(Event event) {
        }

        @Override
        public void globalTabActivated(Event event) {
        }

        @Override
        public void completeInflightOperations() {
        }

        @Override
        public void clearData() {
        }

        @Override
        public void updateSelectors() {
        }

        @Override
        public void redrawScripts() {
            redraws++;
        }
    }

    private static final class StubState implements ApplicationState {
        private final GameData gameData = new GameData();
        final StubUIController controller = new StubUIController();

        @Override
        public GameData getGameData() {
            return gameData;
        }

        @Override
        public void setGameData(GameData newData) {
        }

        @Override
        public ApplicationUIController getApplicationUI() {
            return controller;
        }

        @Override
        public org.badvision.outlaweditor.api.Platform getCurrentPlatform() {
            return null;
        }

        @Override
        public void setCurrentPlatform(org.badvision.outlaweditor.api.Platform p) {
        }

        @Override
        public ApplicationUIController getController() {
            return controller;
        }

        @Override
        public Stage getPrimaryStage() {
            return null;
        }
    }
}
