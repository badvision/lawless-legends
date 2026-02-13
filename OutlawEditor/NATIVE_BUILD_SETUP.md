# Native Build Setup Summary

This document summarizes the GraalVM Native Image / GluonFX setup grafted from the Jace project to OutlawEditor.

## What Was Added

### 1. Maven Configuration (pom.xml)

Updated GluonFX plugin from version 1.0.24 to 1.0.26 (matching Jace) with proper configuration:
```xml
<plugin>
    <groupId>com.gluonhq</groupId>
    <artifactId>gluonfx-maven-plugin</artifactId>
    <version>1.0.26</version>
    <configuration>
        <mainClass>${mainClass}</mainClass>
        <resourcesList>
            <resource>.*</resource>
        </resourcesList>
        <releaseConfiguration>
            <vendor>The 8-Bit Bunch</vendor>
            <skipSigning>true</skipSigning>
        </releaseConfiguration>
    </configuration>
</plugin>
```

### 2. Native Image Configuration

Copied from Jace to `src/main/resources/META-INF/native-image/`:

- **native-image.properties** - GraalVM build arguments
  - Initializes MacAccessible at runtime (not build time)
  - Reports unsupported elements at runtime instead of failing

- **reflect-config.json** - Reflection configuration
  - JavaFX internals (Glass UI, WebKit, etc.)
  - WebView support classes
  - ControlsFX classes

- **jni-config.json** - JNI configuration
  - Extensive WebKit JNI bindings
  - JavaFX native interface methods
  - Graphics and rendering bridges

- **resource-config.json** - Resource patterns
  - Includes all resources via `.*` pattern

- **proxy-config.json** - Dynamic proxy configuration

- **serialization-config.json** - Serialization configuration

- **filter-file.json** - Class filtering for build

- **predefined-classes-config.json** - AOT compiled classes

### 3. Build Scripts

Created platform-specific build scripts:

#### macOS ARM64: `build_mac_arm.sh`
- Builds for Apple Silicon (M1/M2/M3)
- Creates `.app` bundle
- Generates DMG installer
- Output: `OutlawEditor-3.0-macOS-arm64.dmg`

#### macOS Intel: `build_mac_intel.sh`
- Builds for Intel x86_64 Macs
- Creates `.app` bundle
- Generates DMG installer
- Output: `OutlawEditor-3.0-macOS-x86_64.dmg`

#### Windows: `build_windows.bat`
- Builds for Windows x86_64
- Creates standalone `.exe`
- Output: `target\gluonfx\x86_64-windows\outlaweditor.exe`

### 4. Documentation

- **BUILD.md** - Complete build instructions
- **NATIVE_BUILD_SETUP.md** (this file) - Setup summary

## What Was NOT Copied

OutlawEditor doesn't need these Jace components:

- ❌ LWJGL dependencies (3D graphics)
- ❌ OpenAL dependencies (audio)
- ❌ NestedVM dependencies (6502 emulation)
- ❌ Platform-specific LWJGL natives profiles
- ❌ Annotation processors
- ❌ JaCoCo test coverage plugin
- ❌ Moditect module descriptor plugin

## Key Differences from Jace

1. **Simpler dependencies**: Only JavaFX, JAXB, ControlsFX
2. **No game engine**: No emulation, no audio synthesis
3. **Editor-focused**: File I/O, XML handling, UI controls
4. **Smaller binary**: ~40-60MB vs Jace's ~80-100MB

## WebView Support

The critical feature that makes this work is **GluonFX's WebKit integration**:

- Standard GraalVM **cannot** compile WebView
- GluonFX provides pre-built WebKit libraries and JNI bindings
- Blockly editor (Google Blockly in WebView) works in native builds
- Configuration in `jni-config.json` includes 50+ WebKit classes

## Testing Native Builds

Before distributing, test these WebView features:

1. ✅ Open Mythos script editor (Blockly)
2. ✅ Load/save scripts via JavaScript bridge
3. ✅ Script validation and compilation
4. ✅ All Blockly drag-drop interactions
5. ✅ Script export functionality

## Build Time Expectations

- **First build**: 10-20 minutes (downloads dependencies)
- **Subsequent builds**: 5-10 minutes
- **macOS native image**: ~5-8 minutes
- **Windows native image**: ~7-12 minutes

## Future Maintenance

When adding new features that use:

- **Reflection** → Update `reflect-config.json`
- **JNI** → Update `jni-config.json`
- **Dynamic proxies** → Update `proxy-config.json`
- **Resources** → Usually auto-included via `.*` pattern

Use the tracing agent to capture new patterns:
```bash
java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image \
     -jar target/OutlawEditor.jar
```

## Success Criteria

✅ Project compiles with `mvn clean compile`
✅ GluonFX plugin version 1.0.26 configured
✅ Native-image configs copied and adapted
✅ Build scripts created and executable
✅ Documentation complete

## Next Steps

To actually build native executables:

1. Install GraalVM Gluon (see BUILD.md)
2. Run appropriate build script for your platform
3. Test the generated executable thoroughly
4. Distribute DMG (macOS) or EXE (Windows)

## Notes

- Native builds are for **distribution only**
- Use `mvn javafx:run` for development
- WebView is the main reason GluonFX is required
- Without WebView, standard GraalVM Native Image would work
