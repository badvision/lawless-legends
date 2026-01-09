#!/bin/sh
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-gluon-22.1.0.1/Contents/Home
cd ~/Documents/code/lawless-legends/Platform/Apple/tools/jace

# Build native executable
mvn gluonfx:build

# Create .app bundle structure
APP_DIR="target/gluonfx/aarch64-darwin/lawlesslegends.app"
echo "Creating .app bundle structure..."
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"

# Copy executable into bundle
cp target/gluonfx/aarch64-darwin/lawlesslegends "$APP_DIR/Contents/MacOS/"

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

# Copy .app bundle to Desktop
echo "Copying to Desktop..."
cp -R "$APP_DIR" ~/Desktop/

echo "Build complete! lawlesslegends.app is on your Desktop"
