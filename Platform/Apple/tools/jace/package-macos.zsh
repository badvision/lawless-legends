#!/usr/bin/env zsh

# Package-Lawless-Legends-Safe.zsh - A proper packaging script using jpackage to create a self-contained app
# This script uses a single temp directory for all operations to ensure safety

# Ensure PATH is set correctly
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:${HOME}/.jenv/bin:${HOME}/.jenv/shims:$PATH"

# ----- VARIABLES -----
# Create a single temporary directory for all build operations
MAIN_TEMP_DIR="$(/usr/bin/mktemp -d)"
log_prefix="package-$(/bin/date +%Y%m%d-%H%M%S)"

# Define final destination paths - only DMG goes to desktop, everything else uses temp folders
DESKTOP_DIR="${HOME}/Desktop"
LOGS_DIR="${HOME}/Library/Logs/LawlessLegends"
LOG_FILE="${LOGS_DIR}/${log_prefix}.log"

# All build files go in the temp directory
TEMP_APP_DIR="${MAIN_TEMP_DIR}/app_bundle"
TEMP_APP="${TEMP_APP_DIR}/Lawless Legends.app"
TEMP_DMG="${MAIN_TEMP_DIR}/Lawless Legends.dmg"

# Final DMG location (the only file copied to desktop)
FINAL_DMG="${DESKTOP_DIR}/Lawless Legends.dmg"

# Define source paths
SOURCE_CODE_DIR="${HOME}/Documents/code/lawless-legends"
JACE_DIR="${SOURCE_CODE_DIR}/Platform/Apple/tools/jace"
SOURCE_JAR="${JACE_DIR}/target/LawlessLegends.jar"
DEPENDENCY_DIR="${JACE_DIR}/target/dependency"

# Define build paths (all within temp dir)
BUILD_DIR="${MAIN_TEMP_DIR}/build"
PACKAGE_DIR="${MAIN_TEMP_DIR}/package"
JLINK_MODS_DIR="${MAIN_TEMP_DIR}/jlink_mods"
ICON_FILE="${MAIN_TEMP_DIR}/AppIcon.icns"
DMG_TEMP_DIR="${MAIN_TEMP_DIR}/dmg_temp"

# Application info
VERSION="1.0.0"
MAIN_CLASS="jace.LawlessLegends"
JAVA_FX_JMODS=""

# ----- HELPER FUNCTIONS -----

# Log message to both console and log file
log() {
    echo "$1" | /usr/bin/tee -a "${LOG_FILE}"
}

# Some macOS fixes for common commands
# macOS doesn't have /bin/basename - it's in /usr/bin
basename() {
    /usr/bin/basename "$@"
}

# macOS doesn't have /bin/tr - it's in /usr/bin
tr_cmd() {
    /usr/bin/tr "$@"
}

# macOS doesn't have /bin/wc - it's in /usr/bin
wc_cmd() {
    /usr/bin/wc "$@"
}

# Display section header
section() {
    log ""
    log "===== $1 ====="
    log "$(/usr/bin/printf '=%.0s' {1..50})"
}

# Safe exit - cleans up temporary directory
safe_exit() {
    local exit_code=$1
    
    # Only clean up if the main temp dir exists and is under /tmp or /var
    if [[ -d "${MAIN_TEMP_DIR}" && ("${MAIN_TEMP_DIR}" == /tmp/* || "${MAIN_TEMP_DIR}" == /var/*) ]]; then
        log "Cleaning up temporary directory: ${MAIN_TEMP_DIR}"
        /bin/rm -rf "${MAIN_TEMP_DIR}"
    else
        log "⚠️ SAFETY CHECK FAILED - Skipping cleanup - temporary directory is not in a safe location or doesn't exist"
        log "Manual cleanup may be required for: ${MAIN_TEMP_DIR}"
    fi
    
    log "Script completed with exit code: ${exit_code}"
    exit ${exit_code}
}

# Convert PNG to ICNS
convert_png_to_icns() {
    local png_file="$1"
    local icns_file="$2"
    local icon_set_dir="${MAIN_TEMP_DIR}/AppIcon.iconset"

    log "Converting PNG to ICNS format..."
    /bin/mkdir -p "${icon_set_dir}"

    # Generate various icon sizes
    /usr/bin/sips -z 16 16     "$png_file" --out "${icon_set_dir}/icon_16x16.png" > /dev/null 2>&1
    /usr/bin/sips -z 32 32     "$png_file" --out "${icon_set_dir}/icon_16x16@2x.png" > /dev/null 2>&1
    /usr/bin/sips -z 32 32     "$png_file" --out "${icon_set_dir}/icon_32x32.png" > /dev/null 2>&1
    /usr/bin/sips -z 64 64     "$png_file" --out "${icon_set_dir}/icon_32x32@2x.png" > /dev/null 2>&1
    /usr/bin/sips -z 128 128   "$png_file" --out "${icon_set_dir}/icon_128x128.png" > /dev/null 2>&1
    /usr/bin/sips -z 256 256   "$png_file" --out "${icon_set_dir}/icon_128x128@2x.png" > /dev/null 2>&1
    /usr/bin/sips -z 256 256   "$png_file" --out "${icon_set_dir}/icon_256x256.png" > /dev/null 2>&1
    /usr/bin/sips -z 512 512   "$png_file" --out "${icon_set_dir}/icon_256x256@2x.png" > /dev/null 2>&1
    /usr/bin/sips -z 512 512   "$png_file" --out "${icon_set_dir}/icon_512x512.png" > /dev/null 2>&1
    /usr/bin/sips -z 1024 1024 "$png_file" --out "${icon_set_dir}/icon_512x512@2x.png" > /dev/null 2>&1

    # Convert the iconset to icns
    /usr/bin/iconutil -c icns "${icon_set_dir}" -o "${icns_file}"
    
    log "✅ Application icon created from game_icon.png"
}

# ----- STEP 3: PREPARE ICON -----
section "PREPARING APPLICATION ICON"

# Set the app icon using the game_icon.png from source
ICON_PATH="${JACE_DIR}/src/main/resources/jace/data/game_icon.png"

# Create the icon file
if [[ -f "${ICON_PATH}" ]]; then
    convert_png_to_icns "${ICON_PATH}" "${ICON_FILE}"
else
    log "⚠️ Game icon not found at ${ICON_PATH}, using default icon"
    /usr/bin/curl -s -L -o "${ICON_FILE}" "https://raw.githubusercontent.com/badvision/lawless-legends/master/Platform/Apple/resources/AppIcon.icns" 2>/dev/null
fi

# Verify the icon file exists
if [[ ! -f "${ICON_FILE}" ]]; then
    log "❌ Icon file was not created successfully at ${ICON_FILE}"
    log "Checking for alternative icons..."
    
    # Try to find an alternative icon
    ALT_ICON_PATH="${JACE_DIR}/resources/AppIcon.icns"
    if [[ -f "${ALT_ICON_PATH}" ]]; then
        log "Found alternative icon at ${ALT_ICON_PATH}"
        /bin/cp "${ALT_ICON_PATH}" "${ICON_FILE}"
    else
        # Create a simple icon as fallback
        log "Creating a simple fallback icon..."
        /usr/bin/sips -s format icns "${JACE_DIR}/src/main/resources/jace/data/game_icon.png" --out "${ICON_FILE}" > /dev/null 2>&1 || true
    fi
    
    # Final check
    if [[ ! -f "${ICON_FILE}" ]]; then
        log "❌ Failed to create or find a valid icon file. Will proceed without custom icon."
    else
        log "✅ Alternative icon file created at: ${ICON_FILE}"
    fi
else
    log "✅ Icon file exists at: ${ICON_FILE}"
fi

# ----- STEP 4: PREPARE JAVAFX MODULES -----
section "PREPARING JAVAFX MODULES"

# Try to find in common locations first
JAVAFX_JMODS_DIR=""
for location in "${HOME}/Downloads/javafx-jmods-arm64/javafx-jmods-21.0.2" "${HOME}/Downloads/javafx-jmods-21.0.2" "${HOME}/Downloads/javafx-jmods-21" "/opt/javafx-jmods-21.0.2" "/opt/javafx-jmods-21"; do
    if [[ -d "${location}" && -f "${location}/javafx.base.jmod" ]]; then
        JAVAFX_JMODS_DIR="${location}"
        break
    fi
done

# If not found, download directly
if [[ -z "${JAVAFX_JMODS_DIR}" ]]; then
    log "JavaFX jmods not found in common locations, downloading..."
    
    # Set the paths directly without using functions to avoid stdout issues
    JAVAFX_DOWNLOAD_DIR="${MAIN_TEMP_DIR}/javafx-download"
    JAVAFX_EXTRACT_DIR="${MAIN_TEMP_DIR}/javafx-jmods"
    JAVAFX_VERSION="21.0.2"
    JAVAFX_ARCH="aarch64"
    JAVAFX_OS="osx"
    JAVAFX_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${JAVAFX_OS}-${JAVAFX_ARCH}_bin-jmods.zip"
    JAVAFX_ZIP="${JAVAFX_DOWNLOAD_DIR}/javafx-jmods.zip"
    EXPECTED_JAVAFX_DIR="${JAVAFX_EXTRACT_DIR}/javafx-jmods-${JAVAFX_VERSION}"
    
    log "Creating download directory: ${JAVAFX_DOWNLOAD_DIR}"
    /bin/mkdir -p "${JAVAFX_DOWNLOAD_DIR}"
    
    log "Creating extract directory: ${JAVAFX_EXTRACT_DIR}"
    /bin/mkdir -p "${JAVAFX_EXTRACT_DIR}"
    
    log "Downloading JavaFX modules from ${JAVAFX_URL}..."
    curl -L "${JAVAFX_URL}" -o "${JAVAFX_ZIP}" >> "${LOG_FILE}" 2>&1
    
    if [[ ! -f "${JAVAFX_ZIP}" ]]; then
        log "❌ Failed to download JavaFX modules zip file"
        safe_exit 1
    fi
    
    log "Extracting JavaFX modules to ${JAVAFX_EXTRACT_DIR}..."
    unzip -q -o "${JAVAFX_ZIP}" -d "${JAVAFX_EXTRACT_DIR}" >> "${LOG_FILE}" 2>&1
    
    # Verify the extraction succeeded
    if [[ ! -d "${EXPECTED_JAVAFX_DIR}" ]]; then
        log "❌ Failed to extract JavaFX modules to expected directory: ${EXPECTED_JAVAFX_DIR}"
        log "Checking what was extracted:"
        find "${JAVAFX_EXTRACT_DIR}" -type d -maxdepth 2 | while read -r dir; do
            log "  Found: ${dir}"
        done
        safe_exit 1
    fi
    
    JAVAFX_JMODS_DIR="${EXPECTED_JAVAFX_DIR}"
    log "✅ JavaFX modules extracted to: ${JAVAFX_JMODS_DIR}"
else
    log "✅ Found JavaFX jmods at: ${JAVAFX_JMODS_DIR}"
fi

# Confirm the JavaFX modules exist by listing them
log "Checking JavaFX modules in directory:"
ls -la "${JAVAFX_JMODS_DIR}"/*.jmod 2>/dev/null | while read -r line; do 
    log "   ${line}"
done

# Verify specific modules exist
REQUIRED_MODULES=("javafx.base.jmod" "javafx.graphics.jmod" "javafx.controls.jmod" "javafx.fxml.jmod" "javafx.web.jmod" "javafx.media.jmod" "javafx.swing.jmod")
MISSING_MODULES=()

for module in "${REQUIRED_MODULES[@]}"; do
    if [[ ! -f "${JAVAFX_JMODS_DIR}/${module}" ]]; then
        log "⚠️ Warning: ${module} not found in ${JAVAFX_JMODS_DIR}"
        MISSING_MODULES+=("${module}")
    fi
done

if [[ ${#MISSING_MODULES[@]} -gt 0 ]]; then
    log "❌ Missing required JavaFX modules: ${MISSING_MODULES[*]}"
    log "This will cause jlink to fail. Please check the JavaFX jmods package."
    safe_exit 1
fi

# ----- STEP 5: PREPARE RUNTIME IMAGE -----
section "CREATING CUSTOM RUNTIME IMAGE"

# Ensure the directories exist
/bin/mkdir -p "${PACKAGE_DIR}"

# Copy the main JAR file
log "Copying main JAR file..."
if [[ ! -f "${SOURCE_JAR}" ]]; then
    log "❌ Main JAR file not found at ${SOURCE_JAR}"
    log "Checking if we need to build it first..."
    
    # Try to build the JAR if it doesn't exist
    log "Attempting to build with Maven..."
    cd "${JACE_DIR}" && mvn clean package dependency:copy-dependencies >> "${LOG_FILE}" 2>&1
    
    # Check if build was successful
    if [[ ! -f "${SOURCE_JAR}" ]]; then
        log "❌ Failed to build the main JAR file. Cannot proceed."
        safe_exit 1
    else
        log "✅ Successfully built the main JAR file with Maven"
    fi
fi

# Copy the main JAR file
/bin/cp "${SOURCE_JAR}" "${PACKAGE_DIR}/LawlessLegends.jar"

if [[ ! -f "${PACKAGE_DIR}/LawlessLegends.jar" ]]; then
    log "❌ Failed to copy main JAR file to package directory"
    log "Source: ${SOURCE_JAR}"
    log "Destination: ${PACKAGE_DIR}/LawlessLegends.jar"
    safe_exit 1
else
    log "✅ Main JAR file copied successfully"
fi

# Copy all dependencies
log "Copying dependencies..."
if [[ ! -d "${DEPENDENCY_DIR}" ]]; then
    log "❌ Dependency directory not found at ${DEPENDENCY_DIR}"
    log "Attempting to retrieve dependencies with Maven..."
    
    # Try to get dependencies if they don't exist
    cd "${JACE_DIR}" && mvn dependency:copy-dependencies >> "${LOG_FILE}" 2>&1
    
    if [[ ! -d "${DEPENDENCY_DIR}" ]]; then
        log "❌ Failed to retrieve dependencies. Cannot proceed."
        safe_exit 1
    else
        log "✅ Successfully retrieved dependencies with Maven"
    fi
fi

# Count dependencies
DEPS_COUNT=$(/usr/bin/find "${DEPENDENCY_DIR}" -name "*.jar" | /usr/bin/wc -l | tr -d ' ')
log "Found ${DEPS_COUNT} dependencies in ${DEPENDENCY_DIR}"

if [[ $DEPS_COUNT -eq 0 ]]; then
    log "❌ No dependencies found in ${DEPENDENCY_DIR}. Cannot proceed."
    safe_exit 1
fi

# Create a directory for copying dependencies
MODULES_TEMP_DIR="${PACKAGE_DIR}/modules"
/bin/mkdir -p "${MODULES_TEMP_DIR}"

# Copy all JAR files from dependency directory
log "Copying all dependencies to modules directory..."
/usr/bin/find "${DEPENDENCY_DIR}" -name "*.jar" -type f -exec /bin/cp {} "${MODULES_TEMP_DIR}/" \;

# Verify the dependencies were copied
COPIED_COUNT=$(/usr/bin/find "${MODULES_TEMP_DIR}" -name "*.jar" | /usr/bin/wc -l | tr -d ' ')
log "Copied ${COPIED_COUNT} dependencies to ${MODULES_TEMP_DIR}"

if [[ $COPIED_COUNT -eq 0 ]]; then
    log "❌ Failed to copy dependencies. Cannot proceed."
    safe_exit 1
elif [[ $COPIED_COUNT -ne $DEPS_COUNT ]]; then
    log "⚠️ Warning: Not all dependencies were copied (${COPIED_COUNT}/${DEPS_COUNT})"
    log "This may cause issues during runtime."
else
    log "✅ All dependencies copied successfully"
fi

# Find Java home and jmods directory
if [[ -z "${JAVA_HOME}" ]]; then
    JAVA_HOME=$(/usr/libexec/java_home)
    log "Using Java home: ${JAVA_HOME}"
fi

# Define the jlink command using the full path
JLINK_CMD="${JAVA_HOME}/bin/jlink"
log "Using jlink command: ${JLINK_CMD}"

# Define the jpackage command using the full path
JPACKAGE_CMD="${JAVA_HOME}/bin/jpackage"
log "Using jpackage command: ${JPACKAGE_CMD}"

JDK_JMODS="${JAVA_HOME}/jmods"
if [[ ! -d "${JDK_JMODS}" ]]; then
    log "⚠️ Could not find JDK jmods directory at ${JDK_JMODS}"
    # Try to locate jmods using alternatives
    if [[ -d "/Library/Java/JavaVirtualMachines" ]]; then
        LATEST_JDK=$(find /Library/Java/JavaVirtualMachines -type d -name "*.jdk" | sort -r | head -1)
        if [[ -n "${LATEST_JDK}" ]]; then
            JDK_JMODS="${LATEST_JDK}/Contents/Home/jmods"
            log "Using alternative JDK jmods directory: ${JDK_JMODS}"
            # Update JAVA_HOME and jlink command
            JAVA_HOME="${LATEST_JDK}/Contents/Home"
            JLINK_CMD="${JAVA_HOME}/bin/jlink"
            log "Updated Java home: ${JAVA_HOME}"
            log "Updated jlink command: ${JLINK_CMD}"
        fi
    fi
fi

if [[ ! -d "${JDK_JMODS}" ]]; then
    log "❌ Could not find JDK jmods directory. Cannot proceed with jlink."
    safe_exit 1
fi

# Create a module-path for jlink
MODULE_PATH="${JDK_JMODS}:${JAVAFX_JMODS_DIR}"

# Debug the module path
log "Checking if module path directories exist:"
for dir in $(echo "${MODULE_PATH}" | tr ':' ' '); do
    if [[ -d "${dir}" ]]; then
        log "✅ Directory exists: ${dir}"
        log "  Example modules from ${dir}:"
        ls -la "${dir}" | grep .jmod | head -5 | while read -r line; do
            log "    ${line}"
        done
    else
        log "❌ Directory does not exist: ${dir}"
    fi
done

# Double check if javafx.web.jmod is available in the module path
if [[ ! -f "${JAVAFX_JMODS_DIR}/javafx.web.jmod" ]]; then
    log "❌ Critical JavaFX module missing: javafx.web.jmod"
    log "Module path: ${MODULE_PATH}"
    log "This will cause jlink to fail. Cannot proceed."
    safe_exit 1
fi

log "Using module path: ${MODULE_PATH}"

# List the JavaFX modules to add
JAVAFX_ADD_MODULES="javafx.controls,javafx.fxml,javafx.web,javafx.media,javafx.swing,javafx.graphics"
log "Using JavaFX modules: ${JAVAFX_ADD_MODULES}"

# ----- STEP 6: CREATE APPLICATION PACKAGE -----
section "CREATING APPLICATION PACKAGE"

# Prepare launcher arguments to properly handle JavaFX modules
LAUNCHER_ARGS=(
    # Main application properties
    "--name" "Lawless Legends"
    "--app-version" "${VERSION}"
    "--vendor" "The 8-Bit Bunch"
    "--copyright" "Copyright © 2025 The 8-Bit Bunch. All rights reserved."
    "--description" "Lawless Legends - A retro-style RPG game"
    
    # Input and output paths
    "--input" "${PACKAGE_DIR}"
    "--dest" "${TEMP_APP_DIR}"  # Output to temp dir, not desktop
    
    # Main class configuration
    "--main-jar" "LawlessLegends.jar"
    "--main-class" "${MAIN_CLASS}"
    
    # Mac specific options
    "--mac-package-name" "Lawless Legends"
    "--mac-package-identifier" "com.applecorn.lawlesslegends"
    
    # Runtime image configuration
    "--runtime-image" "${JLINK_MODS_DIR}"
    
    # JavaFX configuration
    "--java-options" "--module-path \$APPDIR/modules --add-modules=${JAVAFX_ADD_MODULES}"
    
    # Package type
    "--type" "app-image"
)

# Add icon only if it exists
if [[ -f "${ICON_FILE}" ]]; then
    log "✅ Using icon file: ${ICON_FILE}"
    LAUNCHER_ARGS+=("--icon" "${ICON_FILE}")
else
    log "⚠️ Icon file not found, packaging without custom icon"
fi

# First, create a custom runtime image using jlink
section "CREATING CUSTOM RUNTIME WITH JLINK"
log "Creating custom runtime image with JavaFX modules..."

# Add base JDK and JavaFX modules
JLINK_MODULES="java.base,java.desktop,java.logging,java.management,java.prefs,java.scripting,java.xml,jdk.unsupported,${JAVAFX_ADD_MODULES}"

# Create runtime image
log "Running jlink to create custom runtime with modules: ${JLINK_MODULES}"
${JLINK_CMD} \
  --module-path "${MODULE_PATH}" \
  --add-modules "${JLINK_MODULES}" \
  --strip-debug --compress=2 \
  --no-header-files --no-man-pages \
  --output "${JLINK_MODS_DIR}" 

jlink_exit=$?
if [[ $jlink_exit -ne 0 ]]; then
    log "❌ jlink failed with exit code $jlink_exit. See output above for details."
    safe_exit 1
fi

# Verify the runtime was created
if [[ ! -d "${JLINK_MODS_DIR}/bin" ]]; then
    log "❌ jlink did not create a valid runtime - bin directory is missing"
    safe_exit 1
fi

log "✅ Custom runtime image created successfully at ${JLINK_MODS_DIR}"
log "Content of runtime:"
/bin/ls -la "${JLINK_MODS_DIR}" | while read line; do log "  $line"; done

# Check the architecture of the native libraries
log "Checking architecture of native libraries:"
file "${JLINK_MODS_DIR}/lib/libprism_es2.dylib" 2>/dev/null || log "libprism_es2.dylib not found"
file "${JLINK_MODS_DIR}/lib/libprism_sw.dylib" 2>/dev/null || log "libprism_sw.dylib not found"

# Create a config file for JavaFX VM options
CONFIG_DIR="${PACKAGE_DIR}/config"
/bin/mkdir -p "${CONFIG_DIR}"

/bin/cat > "${CONFIG_DIR}/jvm-options.txt" << EOF
--module-path \$APPDIR/modules
--add-modules=${JAVAFX_ADD_MODULES}
EOF

log "✅ Created JVM options configuration for JavaFX"

# Create application package with jpackage
log "Creating application package with jpackage..."
log "Running jpackage with arguments: ${LAUNCHER_ARGS[*]}"
${JPACKAGE_CMD} "${LAUNCHER_ARGS[@]}"
jpackage_exit=$?

if [[ $jpackage_exit -ne 0 ]]; then
    log "❌ jpackage failed with exit code $jpackage_exit. See output above for details."
    safe_exit 1
fi

# Get the name of the app bundle created in the temp dir
if [[ ! -d "${TEMP_APP}" ]]; then
    log "❌ jpackage did not create the expected application bundle in ${TEMP_APP_DIR}"
    safe_exit 1
fi

log "✅ Application package created successfully at: ${TEMP_APP}"

# ----- STEP 7: COPY ADDITIONAL DEPENDENCIES -----
section "ADDING ADDITIONAL DEPENDENCIES"

# Create modules directory if it doesn't exist
MODULES_DIR="${TEMP_APP}/Contents/app/modules"
/bin/mkdir -p "${MODULES_DIR}"

# First check if the app bundle was created
if [[ ! -d "${TEMP_APP}" ]]; then
    log "❌ Application bundle not found at ${TEMP_APP}"
    log "Cannot add dependencies. jpackage may have failed."
    safe_exit 1
fi

# Simple approach: copy all JAR files from dependency directory
log "Copying all dependencies to modules directory..."
if [[ ! -d "${DEPENDENCY_DIR}" ]]; then
    log "❌ Dependency directory not found at: ${DEPENDENCY_DIR}"
    safe_exit 1
fi

# Copy all JAR files from the dependency directory
/usr/bin/find "${DEPENDENCY_DIR}" -name "*.jar" -type f -exec /bin/cp -f {} "${MODULES_DIR}/" \;

# Verify the dependencies were copied
if [[ ! -d "${MODULES_DIR}" ]]; then
    log "❌ Module directory ${MODULES_DIR} not found in the app bundle!"
    safe_exit 1
fi

# Verify the copy worked
JAVAFX_COUNT=$(/usr/bin/find "${MODULES_DIR}" -name "javafx*.jar" | wc_cmd -l | tr_cmd -d ' ')
LWJGL_COUNT=$(/usr/bin/find "${MODULES_DIR}" -name "lwjgl*.jar" | wc_cmd -l | tr_cmd -d ' ')
OTHER_COUNT=$(/usr/bin/find "${MODULES_DIR}" -name "*.jar" -not -name "javafx*.jar" -not -name "lwjgl*.jar" | wc_cmd -l | tr_cmd -d ' ')
TOTAL_COUNT=$(/usr/bin/find "${MODULES_DIR}" -name "*.jar" | wc_cmd -l | tr_cmd -d ' ')

# Set ownership and permissions for the JAR files
/bin/chmod -R 755 "${MODULES_DIR}"

log "✅ Dependencies copied to modules directory:"
log "   - JavaFX JARs: ${JAVAFX_COUNT}"
log "   - LWJGL JARs: ${LWJGL_COUNT}"
log "   - Other JARs: ${OTHER_COUNT}"
log "   - Total JARs: ${TOTAL_COUNT}"

# Check if the critical JAR files are there
if [[ $JAVAFX_COUNT -eq 0 ]]; then
    log "❌ No JavaFX JAR files were copied. This will cause JavaFX failures."
    safe_exit 1
fi

if [[ $LWJGL_COUNT -eq 0 ]]; then
    log "❌ No LWJGL JAR files were copied. This will cause OpenAL errors."
    safe_exit 1
fi

# Manually list the module directory contents for debugging
log "Contents of modules directory:"
/usr/bin/find "${MODULES_DIR}" -name "*.jar" -type f -exec basename {} \; | while read jar_name; do
    log "   - ${jar_name}"
done

# Create a custom launcher script that properly sets up JavaFX
LAUNCHER_SCRIPT="${TEMP_APP}/Contents/MacOS/Lawless Legends"
log "Creating custom launcher script: ${LAUNCHER_SCRIPT}"

/bin/cat > "${LAUNCHER_SCRIPT}" << 'EOF'
#!/bin/bash

# Determine the application path
APP_PATH=$(cd "$(dirname "$0")/.."; pwd)

# Set up paths
JAVA_HOME="${APP_PATH}/runtime/Contents/Home"
JAVA_EXEC="${JAVA_HOME}/bin/java"

# Set main class
MAIN_CLASS="jace.LawlessLegends"

# Set classpath with main JAR first
MAIN_JAR="${APP_PATH}/app/LawlessLegends.jar"
MODULES_DIR="${APP_PATH}/app/modules"

# Check for test mode
TEST_MODE=0
if [[ "$1" == "--test" ]]; then
    TEST_MODE=1
    echo "💬 Running in diagnostic test mode"
    echo "💬 This mode will print extra debugging information"
fi

# Print diagnostic information
echo "Starting Lawless Legends..."
echo "Application path: ${APP_PATH}"
echo "Using embedded Java runtime: ${JAVA_HOME}"

# Print Java version for debugging
"${JAVA_EXEC}" -version

# List available JARs
echo "Checking required JAR files:"
if [[ ! -f "${MAIN_JAR}" ]]; then
    echo "❌ ERROR: Main JAR file not found at ${MAIN_JAR}"
    exit 1
else
    echo "✅ Main JAR file found: ${MAIN_JAR}"
fi

if [[ ! -d "${MODULES_DIR}" ]]; then
    echo "❌ ERROR: Modules directory not found at ${MODULES_DIR}"
    exit 1
else
    echo "✅ Modules directory found with $(ls -1 ${MODULES_DIR}/*.jar 2>/dev/null | wc -l) JAR files"
fi

if [[ $TEST_MODE -eq 1 ]]; then
    echo "📋 Listing all modules:"
    ls -la ${MODULES_DIR}/*.jar 2>/dev/null | while read line; do echo "   $line"; done
fi

echo "Using main class: ${MAIN_CLASS}"
echo "Adding modules directory to classpath: ${MODULES_DIR}"

# Build classpath with all JARs
CLASSPATH="${MAIN_JAR}"
if [ -d "${MODULES_DIR}" ]; then
    CLASSPATH="${CLASSPATH}:${MODULES_DIR}/*"
fi

echo "Classpath: ${CLASSPATH}"

# Set up native library paths for JavaFX and LWJGL
NATIVE_PATH="${MODULES_DIR}:${JAVA_HOME}/lib:${APP_PATH}/Java:${APP_PATH}/Java/lib"
echo "Native library path: ${NATIVE_PATH}"

# Extract any native libraries from JARs if needed
mkdir -p "${APP_PATH}/Java/lib"
EXTRACTED_COUNT=0
for jar in ${MODULES_DIR}/*-natives-macos-arm64.jar; do
  if [ -f "$jar" ]; then
    echo "Extracting natives from: $(basename $jar)"
    unzip -o -q "$jar" -d "${APP_PATH}/Java/lib" "*.dylib" "*.jnilib" "*.so" 2>/dev/null || true
    EXTRACTED_COUNT=$((EXTRACTED_COUNT+1))
  fi
done

if [[ $EXTRACTED_COUNT -eq 0 && $TEST_MODE -eq 1 ]]; then
    echo "⚠️ WARNING: No native JARs were found for extraction"
fi

# Count extracted natives
EXTRACTED_NATIVES=$(ls -1 "${APP_PATH}/Java/lib"/*.dylib 2>/dev/null | wc -l | tr -d ' ')
echo "Extracted ${EXTRACTED_NATIVES} native libraries"

# List extracted native libraries for debugging
if [ ${EXTRACTED_NATIVES} -gt 0 ]; then
    echo "Native libraries extracted:"
    ls -la "${APP_PATH}/Java/lib/"
fi

# JavaFX system properties - using different options for better compatibility
JAVAFX_OPTIONS="-Dprism.verbose=true -Djavafx.verbose=true -Dprism.order=es2,sw -Dprism.lcdtext=false -Dprism.text=t2k -Djavafx.animation.fullspeed=true -Djavafx.animation.pulse=60 -Djavafx.embed.singleThread=true"

# Check for JavaFX modules
JAVAFX_COUNT=$(ls -1 "${MODULES_DIR}"/javafx*.jar 2>/dev/null | wc -l | tr -d ' ')
if [ ${JAVAFX_COUNT} -gt 0 ]; then
    echo "JavaFX modules found in the modules directory: ${JAVAFX_COUNT} files"
    MODULE_PATH="${MODULES_DIR}"
    echo "Using module path for JavaFX: ${MODULE_PATH}"
    echo "Lawless Legends started at $(date)"
    
    # Add Mac-specific parameters
    if [[ "$(uname)" == "Darwin" ]]; then
        echo "Running on macOS, adding platform-specific options"
        ICON_PATH="${APP_PATH}/Resources/AppIcon.icns"
        if [ -f "${ICON_PATH}" ]; then
            echo "✅ Found icon file at ${ICON_PATH}"
            # Need to properly escape spaces in the dock name
            MAC_OPTIONS="-Xdock:icon=\"${ICON_PATH}\" -Xdock:name=\"Lawless Legends\" -Dapple.awt.application.appearance=system"
        else
            echo "⚠️ Icon file not found at ${ICON_PATH}"
            MAC_OPTIONS=""
        fi
    fi
    
    echo "Running with JavaFX modules and additional options: ${JAVAFX_OPTIONS} ${MAC_OPTIONS}"
    
    # Set both java.library.path AND LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH="${NATIVE_PATH}:${DYLD_LIBRARY_PATH}"
    
    # Add test mode option for debugging
    if [[ $TEST_MODE -eq 1 ]]; then
        # For test mode, run with full debug flags and wait briefly to capture output
        echo "Running with full debug flags in test mode"
        
        # Try running with explicit native library path
        # Note: We need to use a different approach for quotes when using variables with spaces
        if [[ -f "${ICON_PATH}" ]]; then
            "${JAVA_EXEC}" \
                ${JAVAFX_OPTIONS} \
                -Djava.library.path="${NATIVE_PATH}" \
                -Dlwjgl.verbose=true \
                -Dlwjgl.debug=true \
                -XX:+ShowCodeDetailsInExceptionMessages \
                -Xdock:icon="${ICON_PATH}" \
                -Xdock:name="Lawless Legends" \
                -Dapple.awt.application.appearance=system \
                --module-path "${MODULE_PATH}" \
                --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web \
                -classpath "${CLASSPATH}" \
                "${MAIN_CLASS}" "$@" &
        else
            "${JAVA_EXEC}" \
                ${JAVAFX_OPTIONS} \
                -Djava.library.path="${NATIVE_PATH}" \
                -Dlwjgl.verbose=true \
                -Dlwjgl.debug=true \
                -XX:+ShowCodeDetailsInExceptionMessages \
                --module-path "${MODULE_PATH}" \
                --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web \
                -classpath "${CLASSPATH}" \
                "${MAIN_CLASS}" "$@" &
        fi
            
        TEST_PID=$!
        sleep 5
        if ps -p $TEST_PID > /dev/null; then
            echo "✅ Application is still running after 5 seconds - test passed"
            # In test mode, kill the process after a success
            kill $TEST_PID
            exit 0
        else
            echo "❌ Application exited during test mode"
            exit 1
        fi
    else
        # Normal mode - just run the application
        if [[ -f "${ICON_PATH}" ]]; then
            "${JAVA_EXEC}" \
                ${JAVAFX_OPTIONS} \
                -Djava.library.path="${NATIVE_PATH}" \
                -Dlwjgl.verbose=true \
                -Dlwjgl.debug=true \
                -Xdock:icon="${ICON_PATH}" \
                -Xdock:name="Lawless Legends" \
                -Dapple.awt.application.appearance=system \
                --module-path "${MODULE_PATH}" \
                --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web \
                -classpath "${CLASSPATH}" \
                "${MAIN_CLASS}" "$@"
        else
            "${JAVA_EXEC}" \
                ${JAVAFX_OPTIONS} \
                -Djava.library.path="${NATIVE_PATH}" \
                -Dlwjgl.verbose=true \
                -Dlwjgl.debug=true \
                --module-path "${MODULE_PATH}" \
                --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web \
                -classpath "${CLASSPATH}" \
                "${MAIN_CLASS}" "$@"
        fi
    fi
else
    echo "No JavaFX modules found, running without module path"
    echo "Lawless Legends started at $(date)"
    
    # Set both java.library.path AND LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH="${NATIVE_PATH}:${DYLD_LIBRARY_PATH}"
    
    "${JAVA_EXEC}" \
        -Djava.library.path="${NATIVE_PATH}" \
        -Dlwjgl.verbose=true \
        -Dlwjgl.debug=true \
        -classpath "${CLASSPATH}" \
        "${MAIN_CLASS}" "$@"
fi

EXIT_CODE=$?
echo "Lawless Legends exited with code ${EXIT_CODE} at $(date)"
exit ${EXIT_CODE}
EOF

# Make the launcher script executable
/bin/chmod +x "${LAUNCHER_SCRIPT}"

log "✅ Custom launcher script created successfully"
log "✅ Additional dependencies added to the application package"

# ----- STEP 7B: FIX APPLICATION ICON -----
section "FIXING APPLICATION ICON"

# Ensure the icon file exists
if [[ ! -f "${ICON_FILE}" ]]; then
    log "⚠️ Icon file not found at ${ICON_FILE}, attempting to recreate it"
    ICON_PATH="${JACE_DIR}/src/main/resources/jace/data/game_icon.png"
    if [[ -f "${ICON_PATH}" ]]; then
        convert_png_to_icns "${ICON_PATH}" "${ICON_FILE}"
    fi
fi

# Create Resources directory if it doesn't exist
RESOURCES_DIR="${TEMP_APP}/Contents/Resources"
/bin/mkdir -p "${RESOURCES_DIR}"

# Copy the icon to the Resources directory
log "Copying icon to Resources directory..."
/bin/cp "${ICON_FILE}" "${RESOURCES_DIR}/AppIcon.icns"
log "✅ Icon copied to ${RESOURCES_DIR}/AppIcon.icns"

# Verify the icon file exists
if [[ ! -f "${RESOURCES_DIR}/AppIcon.icns" ]]; then
    log "❌ Failed to copy icon to Resources directory"
else
    log "✅ Icon file exists in Resources directory"
    # Show file info (size, permissions)
    /bin/ls -la "${RESOURCES_DIR}/AppIcon.icns" | while read line; do log "  $line"; done
fi

# Update Info.plist with correct icon information
INFO_PLIST="${TEMP_APP}/Contents/Info.plist"
log "Updating Info.plist at ${INFO_PLIST}..."

/bin/cat > "${INFO_PLIST}" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIconName</key>
    <string>AppIcon</string>
    <key>CFBundleName</key>
    <string>Lawless Legends</string>
    <key>CFBundleDisplayName</key>
    <string>Lawless Legends</string>
    <key>CFBundleIdentifier</key>
    <string>com.applecorn.lawlesslegends</string>
    <key>CFBundleVersion</key>
    <string>${VERSION}</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleSignature</key>
    <string>????</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.9.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2025 The 8-Bit Bunch. All rights reserved.</string>
    <key>CFBundleExecutable</key>
    <string>Lawless Legends</string>
</dict>
</plist>
EOF

log "✅ Info.plist updated with proper icon references"

# Show contents of Info.plist for verification
log "Contents of Info.plist:"
/usr/bin/plutil -p "${INFO_PLIST}" | while read line; do log "  $line"; done

# Touch the application bundle to force macOS to refresh icon cache
log "Touching application bundle to refresh icon cache..."
/usr/bin/touch "${TEMP_APP}"
log "✅ Application bundle touched"

# ----- STEP 8: TEST APPLICATION -----
section "TESTING APPLICATION"

log "Testing application thoroughly before creating DMG..."

# Check if icon files are in place
log "Verifying app bundle structure..."
if [[ ! -f "${TEMP_APP}/Contents/Resources/AppIcon.icns" ]]; then
    log "❌ App icon missing from Resources directory"
    safe_exit 1
fi

# Verify Info.plist has icon references
if ! grep -q "CFBundleIconFile" "${TEMP_APP}/Contents/Info.plist"; then
    log "❌ Info.plist missing icon references"
    safe_exit 1
fi

# Verify executable exists and is executable
if [[ ! -x "${TEMP_APP}/Contents/MacOS/Lawless Legends" ]]; then
    log "❌ Executable script not found or not executable"
    safe_exit 1
fi

# Test run the application directly with output capture
log "Running application in test mode with full diagnostics..."
TEST_LOG="${MAIN_TEMP_DIR}/app_test.log"
(cd "${MAIN_TEMP_DIR}" && "${TEMP_APP}/Contents/MacOS/Lawless Legends" --test) > "${TEST_LOG}" 2>&1 &
TEST_PID=$!

log "Application launched with PID: ${TEST_PID}"
log "Waiting 10 seconds to verify application startup..."
/bin/sleep 10

# Check if the application is still running
APP_RUNNING=0
if /bin/ps -p ${TEST_PID} > /dev/null; then
    log "✅ Application started successfully and is still running!"
    APP_RUNNING=1
    log "Killing test instance..."
    /bin/kill ${TEST_PID} 2>/dev/null || true
    /bin/sleep 1
else
    # Check log file for success message
    if grep -q "Application is still running after 5 seconds - test passed" "${TEST_LOG}"; then
        log "✅ Test application exited cleanly after successful test"
        APP_RUNNING=1
    else
        # Application exited - check why
        log "⚠️ Application exited during test. Checking logs for errors..."
        if [[ -f "${TEST_LOG}" ]]; then
            log "--- Start of application test log ---"
            /bin/cat "${TEST_LOG}" | while read line; do log "    $line"; done
            log "--- End of application test log ---"
            
            # Look for common errors in the log
            if grep -q "ClassNotFoundException" "${TEST_LOG}"; then
                log "❌ Class not found error detected. JAR file may be missing required classes."
                safe_exit 1
            elif grep -q "NoClassDefFoundError" "${TEST_LOG}"; then
                log "❌ Missing class dependency error detected. JAR dependencies may be incomplete."
                safe_exit 1
            elif grep -q "UnsatisfiedLinkError" "${TEST_LOG}"; then
                log "❌ Native library error detected. Required native libraries may be missing."
                safe_exit 1
            fi
        else
            log "❌ No test log was produced"
        fi
        
        log "❌ Application testing failed - exiting"
        safe_exit 1
    fi
fi

# ----- STEP 10: CREATE DMG -----
section "CREATING DMG IMAGE"

if [[ ${APP_RUNNING} -eq 1 ]]; then
    log "Application test passed ✅ - proceeding with DMG creation"
else
    log "⚠️ Application test failed - DMG creation skipped"
    safe_exit 1
fi

# Create DMG using jpackage
log "Creating DMG file..."

# Prepare DMG contents
log "Preparing DMG contents..."
/bin/mkdir -p "${DMG_TEMP_DIR}"
/bin/cp -R "${TEMP_APP}" "${DMG_TEMP_DIR}/"
/bin/ln -s /Applications "${DMG_TEMP_DIR}/Applications"

# Copy README file from external source
README_SOURCE="${JACE_DIR}/readme-macos.txt"
if [[ -f "${README_SOURCE}" ]]; then
    log "Using external README file from ${README_SOURCE}"
    /bin/cp "${README_SOURCE}" "${DMG_TEMP_DIR}/README.txt"
else
    log "⚠️ External README file not found at ${README_SOURCE}. Using default text."
    # Create a default README file
    /bin/cat > "${DMG_TEMP_DIR}/README.txt" << EOF
Lawless Legends
--------------

To install:
1. Drag the Lawless Legends app to the Applications folder
2. Double-click the app to play
3. Enjoy!

This application includes its own Java runtime - no need to install Java separately.
EOF
fi

# Generate DMG file
log "Creating DMG file with hdiutil..."
/usr/bin/hdiutil create \
  -volname "Lawless Legends ${VERSION}" \
  -srcfolder "${DMG_TEMP_DIR}" \
  -ov -format UDZO \
  "${TEMP_DMG}"

dmg_exit=$?
if [[ $dmg_exit -ne 0 ]]; then
    log "❌ DMG creation failed with exit code $dmg_exit. See output above for details."
    safe_exit 1
else
    log "✅ DMG file created successfully in temp directory"
fi

# Copy the DMG to the desktop
log "Copying DMG file to desktop..."
/bin/cp "${TEMP_DMG}" "${FINAL_DMG}"
if [[ $? -ne 0 ]]; then
    log "❌ Failed to copy DMG to desktop"
    safe_exit 1
fi

log "✅ DMG file copied to desktop at: ${FINAL_DMG}"

# ----- FINAL SUMMARY -----
section "SUMMARY"

log "✅ Lawless Legends has been successfully packaged!"
log "📦 Application bundle created in temporary directory"
log "💿 DMG disk image: ${FINAL_DMG}"
log "📝 Log directory: ${LOGS_DIR}"
log ""
log "📋 This version includes its own Java runtime - users DO NOT need Java installed!"
log "   The application is completely self-contained with all dependencies included."
log ""
log "You can distribute the DMG file to users who can then:"
log "1. Mount the DMG"
log "2. Drag the application to their Applications folder"
log "3. Run the application (no Java installation required)"

# Clean up the temp directory
safe_exit 0
