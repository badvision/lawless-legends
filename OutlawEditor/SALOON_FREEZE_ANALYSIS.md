
# OutlawEditor Saloon Script Freeze - Technical Analysis

## Executive Summary
The Saloon script freeze/empty workspace issue is caused by a combination of:
1. **Critical**: `stateProperty().addListener` callback fires on WebKit I/O thread, 
   not JavaFX Application Thread (FX Thread). Calling `executeScript` from WebKit 
   thread can cause race conditions, deadlocks, or silent failures on Windows WebView.
2. **Major**: JAXB marshalling of large scripts (329+ blocks) blocks the FX thread 
   during `generateLoadScript()`, causing UI freeze.
3. **Minor**: `workspace.updateToolbox()` API removed in Blockly v12 - silently 
   fails but doesn't crash.

## Data Profile
- **Saloon script**: 329 blocks, ~33KB generated XML
- **JAXB unmarshalling**: Works fine (confirmed via harness)
- **Block type distribution**:
  - text: 75, math_number: 26, variables_get: 27, logic_compare: 29
  - text_println: 37, text_print: 29, controls_if: 11
  - text_getanykey: 27, text_clear_window: 18
  - interaction_*: 28 blocks
  - graphics_set_portrait: 5

## Root Cause Analysis

### Issue 1: executeScript on WebKit Thread (CRITICAL)
**File**: `MythosScriptEditorController.java`, lines 62-104

```java
public void setEditor(MythosEditor editor) {
    final String loadScript = editor.generateLoadScript();
    if (loadScript != null) {
        editorView.getEngine().getLoadWorker().stateProperty().addListener(
            (value, old, newState) -> {
                if (newState == State.SUCCEEDED) {
                    // THIS FIRES ON WEBKIT I/O THREAD, NOT FX THREAD!
                    mythos.setMember("editor", editor);
                    editorView.getEngine().executeScript(
                        "(function(){ try { " + loadScript + " } catch(e) { ... } })()");
                }
            });
    }
    editorView.getEngine().load(MYTHOS_EDITOR);
}
```

**Problem**: According to JavaFX WebView documentation, `stateProperty().addListener` 
callbacks fire on the WebKit I/O thread, not the JavaFX Application Thread. Calling 
`executeScript()` from the WebKit thread is:
- **Thread-unsafe**: WebView's JS engine may not handle cross-thread calls gracefully
- **Platform-specific**: Windows WebView/WebKit behaves differently than macOS
- **Can cause silent failure**: No exception thrown, just no script execution

**Impact**: The `Mythos.setScriptXml()` call never executes or fails silently, 
leaving the workspace empty.

### Issue 2: FX Thread Blocking (MAJOR)
**File**: `MythosScriptEditorController.java`, line 64

```java
final String loadScript = editor.generateLoadScript();
```

`generateLoadScript()` does JAXB marshalling of the entire block tree:
```java
JAXBContext context = JAXBContext.newInstance(Block.class);
context.createMarshaller().marshal(root, buffer);
```

For the Saloon script (329 blocks), this generates ~33KB of XML synchronously on 
the FX thread. Combined with:
- FXML loading
- WebView initialization
- WebKit JS engine startup

...this creates a significant UI freeze window, especially on Windows.

### Issue 3: updateToolbox API Removal (MINOR)
**File**: `mythos_uncompressed.js`, line 55

```javascript
Mythos.workspace.updateToolbox(document.getElementById('toolbox'));
```

Blockly v12 removed the `updateToolbox()` method. Calling it returns undefined 
without throwing, so the toolbox isn't updated but the editor still works.

### Issue 4: Mutex Race Condition (MODERATE)
**File**: `MythosEditor.java`, lines 92-135

```java
synchronized (activeEditors) {
    MythosEditor existingEditor = activeEditors.get(script);
    if (existingEditor != null && existingEditor.isShowing) {
        // ...
        return;
    }
    activeEditors.put(script, this);  // Registered but window may not show
}

// ... later, in try block:
controller.setEditor(this);  // Can throw after registration
```

If `controller.setEditor(this)` throws (e.g., during JAXB marshalling), the editor 
is registered but the window never shows. The `isShowing` flag is already true 
(line 113), but the stage never displays.

## Proposed Fixes

### Fix 1: Execute Script on FX Thread (CRITICAL - Most Likely Root Cause)
Wrap `executeScript` calls in `Platform.runLater()`:

```java
editorView.getEngine().getLoadWorker().stateProperty().addListener(
    (value, old, newState) -> {
        if (newState == State.SUCCEEDED) {
            javafx.application.Platform.runLater(() -> {
                try {
                    mythos = (JSObject) editorView.getEngine().executeScript("Mythos");
                    if (mythos != null) {
                        mythos.setMember("editor", editor);
                        editorView.getEngine().executeScript(
                            "(function(){ try { " + loadScript + " } catch(e) { Mythos.editor.log('Mythos load script failed: ' + e); } })()");
                    }
                } catch (Exception ex) {
                    editor.log("Failed to initialize the Mythos editor page: " + ex);
                }
            });
        }
    });
```

### Fix 2: Offload JAXB Marshalling to Background Thread (MAJOR)
Use `Platform.runLater` or a separate thread for `generateLoadScript()`:

```java
public void setEditor(MythosEditor editor) {
    // Generate load script off FX thread
    String[] loadScriptHolder = new String[1];
    javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
        @Override
        protected String call() {
            return editor.generateLoadScript();
        }
    };
    task.setOnSucceeded(e -> {
        final String loadScript = loadScriptHolder[0] = task.getValue();
        // ... rest of setup with loadScript
    });
    // Start task
    new Thread(task).start();
}
```

### Fix 3: Guard Against DOMParser Failure (Defensive)
Add error handling around `domToWorkspace`:

```javascript
setScriptXml: function (xml) {
    var ws = Mythos.workspace;
    ws.clear();
    try {
        var dom = new DOMParser().parseFromString(xml, 'text/xml').documentElement;
        if (!dom || dom.tagName !== 'xml') {
            throw new Error('Failed to parse XML');
        }
        Blockly.Xml.domToWorkspace(dom, ws);
    } catch(e) {
        console.error('Failed to load script XML:', e);
        return; // Leave workspace empty with error visible
    }
    // ... rest
}
```

### Fix 4: Fix updateToolbox for Blockly v12 (Minor)
Check method existence:
```javascript
if (typeof Mythos.workspace.updateToolbox === 'function') {
    Mythos.workspace.updateToolbox(document.getElementById('toolbox'));
}
```

### Fix 5: Improve Mutex Safety (Moderate)
Don't register in activeEditors until the stage is actually showing:
```java
// Remove early registration, add to activeEditors after primaryStage.show()
primaryStage.show();
synchronized (activeEditors) {
    activeEditors.put(script, this);
}
```

## Testing Recommendation
1. Apply Fix 1 first - most likely causes empty workspace on Windows
2. Verify by running on Windows and checking if Saloon loads correctly
3. If freeze persists, apply Fix 2 to eliminate FX thread blocking
4. Use `javafx.concurrent.Task` for all long-running operations

## File Locations
- `MythosEditor.java`: `/Users/brobert/Documents/code/lawless-legends/OutlawEditor/src/main/java/org/badvision/outlaweditor/ui/impl/MythosEditor.java`
- `MythosScriptEditorController.java`: `/Users/brobert/Documents/code/lawless-legends/OutlawEditor/src/main/java/org/badvision/outlaweditor/ui/impl/MythosScriptEditorController.java`
- `mythos_uncompressed.js`: `/Users/brobert/Documents/code/lawless-legends/OutlawEditor/src/main/resources/mythos/mythos-editor/js/mythos_uncompressed.js`
- `editor.html`: `/Users/brobert/Documents/code/lawless-legends/OutlawEditor/src/main/resources/mythos/mythos-editor/html/editor.html`
