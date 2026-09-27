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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.badvision.outlaweditor.data.xml.Script;
import org.junit.Test;

/**
 * Lifecycle contract for the per-script Mythos editor windows: a script that was
 * opened and then closed must be reopenable directly, and a duplicate open of a
 * script whose window is actually still showing must bounce to the existing window.
 *
 * <p>The registry/state holder is exercised here instead of a real Stage: the window
 * bookkeeping that previously lived inline in {@link MythosEditor#show()}/{@link
 * MythosEditor#close()} is exactly what this contract covers.
 */
public class MythosEditorLifecycleTest {

    private static Script newScript() {
        Script script = new Script();
        script.setName("Lifecycle_Script");
        return script;
    }

    /**
     * Regression test for the "open a script, close it, cannot open it again unless a
     * different script is opened first" bug: close() used to defer all bookkeeping to a
     * later FX pulse, so a reopen of the same script saw the stale registry entry
     * (isShowing still true) and bounced to the dying window instead of proceeding.
     */
    @Test
    public void reopenAfterCloseProceeds() throws InterruptedException {
        Script script = newScript();

        MythosEditor first = new MythosEditor(script, null);
        assertNull("first open must register the editor", MythosEditor.registry.acquire(script, first));
        first.isShowing = true; // the stage is up

        // When another test already started the toolkit, give close() a real
        // stage so its deferred teardown cannot throw on the FX thread. When
        // headless, the bookkeeping remains testable without a Stage.
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.runLater(() -> {
                first.primaryStage = new Stage();
                first.primaryStage.setScene(new Scene(new Pane()));
                ready.countDown();
            });
            assertTrue("FX stage fixture timed out", ready.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException toolkitUnavailable) {
            // No FX toolkit; Platform.runLater will reject close's stage teardown.
        }

        try {
            first.close();
        } catch (IllegalStateException toolkitUnavailable) {
            if (first.primaryStage != null) {
                throw toolkitUnavailable;
            }
        }

        MythosEditor reopened = new MythosEditor(script, null);
        assertNull(
            "a reopen right after close() must proceed, not bounce to a stale/dying window",
            MythosEditor.registry.acquire(script, reopened));
        assertTrue("the reopened editor must be registered", MythosEditor.registry.isRegistered(script));

        // Clean up shared registry state.
        reopened.isShowing = false;
        MythosEditor.registry.release(script);
    }

    /** Opening the same script while its window is actually still showing bounces to it. */
    @Test
    public void duplicateOpenWhileShowingBouncesToExistingWindow() {
        Script script = newScript();

        MythosEditor first = new MythosEditor(script, null);
        assertNull(MythosEditor.registry.acquire(script, first));
        first.isShowing = true;

        MythosEditor duplicate = new MythosEditor(script, null);
        assertSame(
            "a duplicate open must bounce to the editor that is actually showing",
            first,
            MythosEditor.registry.acquire(script, duplicate));
        assertTrue("the showing editor must remain registered", MythosEditor.registry.isRegistered(script));
        assertTrue("the showing editor must not have been displaced", first.isShowing);
        assertFalse("the bounced editor must not count as showing", duplicate.isShowing);

        first.isShowing = false;
        MythosEditor.registry.release(script);
    }

    /**
     * A registered-but-not-showing editor (mid-close or stale) must be replaced by the
     * new instance, never bounced to: this is the race that made a fast reopen after
     * close land on the dying window.
     */
    @Test
    public void registeredButNotShowingEditorIsReplaced() {
        Script script = newScript();

        MythosEditor stale = new MythosEditor(script, null);
        assertNull(MythosEditor.registry.acquire(script, stale));
        // stale: registered, but its window is not (and never was in this test) showing

        MythosEditor fresh = new MythosEditor(script, null);
        assertNull(
            "a registered-but-not-showing editor must be replaced, not bounced to",
            MythosEditor.registry.acquire(script, fresh));
        assertTrue("the fresh editor must be the registered one", MythosEditor.registry.isRegistered(script));

        MythosEditor.registry.release(script);
    }
}
