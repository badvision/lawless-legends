/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor.ui.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.MythosEditor;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.junit.Assume;
import org.junit.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

/** The rendered map-script list follows a Mythos title change without replacing Script objects. */
public class MapEditorTabControllerImplTest {

    private static boolean startToolkit() {
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
            return started.await(10, TimeUnit.SECONDS);
        } catch (Throwable headless) {
            return false;
        }
    }

    private static void onFxThread(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("JavaFX thread did not finish");
        }
        if (failure.get() != null) {
            throw new AssertionError("JavaFX action failed", failure.get());
        }
    }

    private static ListCell<?> visibleCell(Node root) {
        if (root instanceof ListCell && !((ListCell<?>) root).isEmpty()) {
            return (ListCell<?>) root;
        }
        if (root instanceof Parent) {
            for (Node child : ((Parent) root).getChildrenUnmodifiable()) {
                ListCell<?> cell = visibleCell(child);
                if (cell != null) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static Label renderedLabel(Node root) {
        if (root instanceof Label) {
            return (Label) root;
        }
        if (root instanceof Parent) {
            for (Node child : ((Parent) root).getChildrenUnmodifiable()) {
                Label label = renderedLabel(child);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    @Test
    public void scriptListCellReflectsInPlaceRename() throws Exception {
        Assume.assumeTrue("JavaFX toolkit unavailable (headless)", startToolkit());

        Map map = new Map();
        Script script = new Script();
        script.setName("Saloon");
        Script.LocationTrigger location = new Script.LocationTrigger();
        location.setX(3);
        location.setY(7);
        script.getLocationTrigger().add(location);
        map.setScripts(new Scripts());
        map.getScripts().getScript().add(script);

        final ListView<Script>[] list = new ListView[1];
        final Stage[] stage = new Stage[1];
        ApplicationUIController controller = new ApplicationUIControllerImpl() {
            @Override
            public void redrawScripts() {
                // Same population seam used by redrawMapScripts().
                MapEditorTabControllerImpl.populateScripts(list[0], map.getScripts().getScript());
            }
        };
        Application.applicationStateSingleton = new ApplicationState() {
            private final GameData data = new GameData();

            @Override public GameData getGameData() { return data; }
            @Override public void setGameData(GameData value) { }
            @Override public ApplicationUIController getApplicationUI() { return controller; }
            @Override public org.badvision.outlaweditor.api.Platform getCurrentPlatform() { return null; }
            @Override public void setCurrentPlatform(org.badvision.outlaweditor.api.Platform value) { }
            @Override public ApplicationUIController getController() { return controller; }
            @Override public Stage getPrimaryStage() { return null; }
        };

        try {
            onFxThread(() -> {
                ListView<Script> view = new ListView<>();
                view.setCellFactory(v -> new ListCell<Script>() {
                    @Override
                    protected void updateItem(Script item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty || item == null ? null : new Label(item.getName()));
                    }
                });
                list[0] = view;
                MapEditorTabControllerImpl.populateScripts(view, map.getScripts().getScript());
                Stage window = new Stage();
                window.setScene(new Scene(view, 220, 60));
                window.show();
                stage[0] = window;
            });

            // Read the actual Label graphic in the realized ListCell. If the helper
            // cannot find it, fail rather than treating a missing cell as a pass.
            onFxThread(() -> {
                ListCell<?> cell = visibleCell(list[0]);
                assertNotNull("map list must realize a non-empty script cell", cell);
                Label label = renderedLabel(cell);
                assertNotNull("rendered script cell must contain a Label", label);
                assertEquals("Saloon", label.getText());
            });

            MythosEditor editor = new MythosEditor(script, null);
            onFxThread(() -> {
                editor.setFunctionName("Saloon_Renamed");
                ListCell<?> cell = visibleCell(list[0]);
                assertNotNull("redraw must retain a visible script cell", cell);
                Label label = renderedLabel(cell);
                assertNotNull("redraw must retain a rendered Label", label);
                assertEquals("Saloon_Renamed", label.getText());
            });
            assertSame("the map must retain the exact Script instance", script, map.getScripts().getScript().get(0));
            assertSame("rename must preserve its location trigger", location, script.getLocationTrigger().get(0));
        } finally {
            if (stage[0] != null) {
                onFxThread(() -> stage[0].close());
            }
            Application.applicationStateSingleton = null;
        }
    }
}
