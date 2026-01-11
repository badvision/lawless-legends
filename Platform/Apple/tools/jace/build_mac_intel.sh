#!/bin/sh
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-17-gluon-22.1.0.1/Contents/Home
cd ~/Documents/code/lawless-legends/Platform/Apple/tools/jace

# Build native executable
echo "Building native executable..."
mvn gluonfx:build

# Create .app bundle structure
APP_DIR="target/gluonfx/x86_64-darwin/lawlesslegends.app"
echo "Creating .app bundle structure..."
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"

# Copy executable into bundle
cp target/gluonfx/x86_64-darwin/lawlesslegends "$APP_DIR/Contents/MacOS/"

# Copy icon
cp src/main/resources/jace/data/icon.icns "$APP_DIR/Contents/Resources/lawlesslegends.icns"

# Create Info.plist
cat > "$APP_DIR/Contents/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>lawlesslegends</string>
    <key>CFBundleIconFile</key>
    <string>lawlesslegends.icns</string>
    <key>CFBundleIdentifier</key>
    <string>org.8bitbunch.lawlesslegends</string>
    <key>CFBundleName</key>
    <string>Lawless Legends</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>3.1</string>
    <key>CFBundleVersion</key>
    <string>3.1</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>11.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>
EOF

echo ".app bundle created successfully"

# Create DMG
echo "Creating DMG installer..."
DMG_TEMP="/tmp/lawless-dmg-$$"
DMG_NAME="LawlessLegends-3.1-macOS-intel.dmg"

# Create temporary directory for DMG contents
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

# Copy app bundle to DMG contents
cp -R "$APP_DIR" "$DMG_TEMP/"

# Copy README
cp README.txt "$DMG_TEMP/"

# Create Applications folder symlink
ln -s /Applications "$DMG_TEMP/Applications"

# Create the DMG
echo "Packaging DMG..."

# Unmount any existing "Lawless Legends" volumes
hdiutil detach "/Volumes/Lawless Legends" 2>/dev/null || true
sleep 1

cd target/gluonfx/x86_64-darwin
rm -f "$DMG_NAME"
hdiutil create -volname "Lawless Legends" \
    -srcfolder "$DMG_TEMP" \
    -ov -format UDZO \
    "$DMG_NAME"
cd - > /dev/null

# Copy DMG to Desktop
echo "Copying DMG to Desktop..."
cp "target/gluonfx/x86_64-darwin/$DMG_NAME" ~/Desktop/

# Clean up
rm -rf "$DMG_TEMP"

echo ""
echo "════════════════════════════════════════════════════════════"
echo "Build complete!"
echo "DMG created: ~/Desktop/$DMG_NAME"
echo "════════════════════════════════════════════════════════════"
