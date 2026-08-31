
# OutlawEditor Saloon Script Freeze - Investigation Summary

## What Was Done
1. **Profiled world.xml structure** using a custom Java harness (SimpleLoadHarness + SaloonHarness)
2. **Identified root causes** through code analysis of MythosEditor and MythosScriptEditorController
3. **Implemented critical fixes** to address the freeze/empty window issues
4. **Verified the build** compiles successfully

## Data Profile
- **Saloon script**: 329 blocks (largest in game), ~33KB generated XML
- **JAXB unmarshalling**: Works correctly (confirmed via harness)
- **Block types**: text(75), variables_get(27), math_number(26), logic_compare(29),
  text_println(37), controls_if(11), text_getanykey(27), interaction*(28), etc.

## Root Causes Identified

### 1. executeScript Called from WebKit I/O Thread (CRITICAL - Root Cause)
**File**: `MythosScriptEditorController.java`, lines 66-95

The `stateProperty().addListener` callback fires on the WebKit I/O thread, not the 
JavaFX Application Thread. Calling `executeScript()` from this thread causes:
- Race conditions on Windows WebView/WebKit
- Silent script execution failures (empty workspace)
- Potential deadlocks

**FIX APPLIED**: Wrapped the entire try-catch-finally block in `Platform.runLater()`:
```java
if (newState == State.SUCCEEDED) {
    javafx.application.Platform.runLater(() -> {
        try { ... } catch (Exception ex) { ... } finally { ... }
    });
}
```

### 2. FX Thread Blocking During JAXB Marshalling (MAJOR - Contributes to Freeze)
**File**: `MythosScriptEditorController.java`, line 64

`generateLoadScript()` marshals 329-block trees synchronously on FX thread (~33KB XML),
causing noticeable UI freeze on large scripts.

**RECOMMENDED FUTURE FIX**: Offload JAXB marshalling to a background thread.

### 3. updateToolbox API Removed in Blockly v12 (MINOR)
**File**: `mythos_uncompressed.js`, line 55

Blockly v12 removed `workspace.updateToolbox()`. Calling it silently fails (returns 
undefined) but doesn't crash.

**FIX APPLIED**: Added existence check:
```javascript
if (typeof Mythos.workspace.updateToolbox === 'function') {
    Mythos.workspace.updateToolbox(document.getElementById('toolbox'));
}
```

## Files Modified
1. `OutlawEditor/src/main/java/org/badvision/outlaweditor/ui/MythosScriptEditorController.java`
   - Added `Platform.runLater()` wrapper for executeScript calls

2. `src/main/resources/mythos/mythos-editor/js/mythos_uncompressed.js`
   - Added typeof check for updateToolbox before calling

## Files Created (for reference)
- `/Users/brobert/Documents/code/lawless-legends/OutlawEditor/SALOON_FREEZE_ANALYSIS.md` - Detailed analysis
- `/tmp/world.xml` - Copy of world.xml for testing
- `/tmp/saloon-full.xml` - Extracted Saloon script XML
- `/tmp/saloon-loadscript.txt` - Generated loadScript for Saloon

## Testing Recommendation
1. Build: `mvn package`
2. Run on Windows: `java -jar target/OutlawEditor.jar`
3. Load world.xml → open Saloon script
4. Verify: Script editor shows blocks (not empty)

## Additional Observations
- Multiple scripts have 200+ blocks (Don Oro Special: 217, Fremont Special*: 210, 
  Ktotus Special: 216, Dr Pyre SPECIAL: 212)
- Windows WebView/WebKit is more sensitive to cross-thread executeScript calls
- The activeEditors mutex in MythosEditor is correctly synchronized, but has a 
  minor design issue (early registration before show())
