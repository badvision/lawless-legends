# Building OutlawEditor Native Executables

OutlawEditor can be compiled to native executables for macOS and Windows using GraalVM and GluonFX.

## Prerequisites

### For All Platforms

1. **Maven** (3.6+)
2. **GraalVM with GluonFX support**
   - Download from: https://gluonhq.com/products/mobile/
   - Recommended version: GraalVM Gluon 22.1.0.1

### macOS

- macOS 11.0 (Big Sur) or later
- Xcode Command Line Tools: `xcode-select --install`

### Windows

- Windows 10 or later
- Visual Studio 2019 or later (with C++ build tools)
- Windows SDK

## Installation

### Install GraalVM Gluon

#### macOS
```bash
# Download and extract to /Library/Java/JavaVirtualMachines/
# Or use your preferred location and update GRAALVM_HOME in build scripts
```

#### Windows
```cmd
# Download and extract to C:\Program Files\GraalVM\
# Or use your preferred location and update GRAALVM_HOME in build_windows.bat
```

## Building

### macOS ARM64 (Apple Silicon)

```bash
./build_mac_arm.sh
```

This creates:
- `OutlawEditor.app` - Application bundle
- `OutlawEditor-3.0-macOS-arm64.dmg` - DMG installer

### macOS Intel x86_64

```bash
./build_mac_intel.sh
```

This creates:
- `OutlawEditor.app` - Application bundle
- `OutlawEditor-3.0-macOS-x86_64.dmg` - DMG installer

### Windows x86_64

```cmd
build_windows.bat
```

This creates:
- `target\gluonfx\x86_64-windows\outlaweditor.exe` - Native executable

To create a Windows installer, use tools like:
- **Inno Setup** (recommended): https://jrsoftware.org/isinfo.php
- **WiX Toolset**: https://wixtoolset.org/
- **NSIS**: https://nsis.sourceforge.io/

## Build Configuration

### Native Image Configuration

The native image configuration is located in:
```
src/main/resources/META-INF/native-image/
```

Key files:
- `native-image.properties` - GraalVM build arguments
- `reflect-config.json` - Reflection configuration
- `jni-config.json` - JNI configuration
- `resource-config.json` - Resource inclusion patterns

These were adapted from the Jace project and tuned for OutlawEditor's needs.

### Updating Configuration

If you add new features that use reflection, JNI, or dynamic loading, you may need to regenerate configs:

```bash
# Run with the tracing agent
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/OutlawEditor.jar

# Use the app thoroughly to exercise all code paths
# Then rebuild with the updated configuration
```

## Troubleshooting

### Build Fails on macOS

**Error: "Cannot find webkit libraries"**
- Ensure you're using GraalVM Gluon (not standard GraalVM)
- GluonFX includes pre-built WebKit libraries for native image

**Error: "Unsupported architecture"**
- Use `build_mac_arm.sh` on Apple Silicon
- Use `build_mac_intel.sh` on Intel Macs

### Build Fails on Windows

**Error: "LINK : fatal error LNK1104: cannot open file 'MSVCRT.lib'"**
- Install Visual Studio C++ Build Tools
- Ensure Windows SDK is installed

### Runtime Errors

**Error: "UnsatisfiedLinkError" or "ClassNotFoundException"**
- Check native-image configuration files
- Re-run with tracing agent to capture missing entries

**WebView not working**
- Verify you're using GluonFX (not standard GraalVM)
- Check that WebKit configs are present in jni-config.json

## File Size

Expected bundle sizes:
- macOS: ~40-60MB (includes JavaFX + WebKit)
- Windows: ~35-55MB

This is significantly smaller than bundling a full JRE (~100-150MB with jpackage).

## Development vs Native Build

For development, use regular Maven:
```bash
mvn clean javafx:run
```

Only build native images for distribution/testing final builds.

## Notes

- Native builds take 5-15 minutes depending on your system
- First build downloads dependencies and may take longer
- Subsequent builds are faster with Maven's cache
- WebView functionality is fully supported via GluonFX's webkit integration
