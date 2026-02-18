#!/bin/sh
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-gluon-22.1.0.1/Contents/Home
cd "$(dirname "$0")"

# Build native executable
echo "Building native executable for macOS Intel x86_64..."
mvn clean package
mvn gluonfx:build

# Create .app bundle structure
APP_DIR="target/gluonfx/x86_64-darwin/OutlawEditor.app"
echo "Creating .app bundle structure..."
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"

# Copy executable into bundle
cp target/gluonfx/x86_64-darwin/outlaweditor "$APP_DIR/Contents/MacOS/"

# Copy icon if it exists
if [ -f "src/main/resources/icon.icns" ]; then
    cp src/main/resources/icon.icns "$APP_DIR/Contents/Resources/OutlawEditor.icns"
fi

# Create Info.plist
cat > "$APP_DIR/Contents/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>outlaweditor</string>
    <key>CFBundleIconFile</key>
    <string>OutlawEditor.icns</string>
    <key>CFBundleIdentifier</key>
    <string>org.badvision.outlaweditor</string>
    <key>CFBundleName</key>
    <string>OutlawEditor</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>3.0</string>
    <key>CFBundleVersion</key>
    <string>3.0</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeExtensions</key>
            <array>
                <string>xml</string>
            </array>
            <key>CFBundleTypeName</key>
            <string>Game Data File</string>
            <key>CFBundleTypeRole</key>
            <string>Editor</string>
        </dict>
    </array>
</dict>
</plist>
EOF

echo ".app bundle created successfully"

# Create DMG
echo "Creating DMG installer..."
DMG_TEMP="/tmp/outlaweditor-dmg-$$"
DMG_NAME="OutlawEditor-3.0-macOS-x86_64.dmg"

# Create temporary directory for DMG contents
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

# Copy app bundle to DMG contents
cp -R "$APP_DIR" "$DMG_TEMP/"

# Create Applications folder symlink
ln -s /Applications "$DMG_TEMP/Applications"

# Create the DMG
echo "Packaging DMG..."

# Unmount any existing "OutlawEditor" volumes
hdiutil detach "/Volumes/OutlawEditor" 2>/dev/null || true
sleep 1

cd target/gluonfx/x86_64-darwin
rm -f "$DMG_NAME"
hdiutil create -volname "OutlawEditor" \
    -srcfolder "$DMG_TEMP" \
    -ov -format UDZO \
    "$DMG_NAME"
cd - > /dev/null

# Copy DMG to project root
echo "Copying DMG to project root..."
cp "target/gluonfx/x86_64-darwin/$DMG_NAME" ./

# Clean up
rm -rf "$DMG_TEMP"

echo ""
echo "════════════════════════════════════════════════════════════"
echo "Build complete!"
echo "DMG created: $(pwd)/$DMG_NAME"
echo "════════════════════════════════════════════════════════════"
