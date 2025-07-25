# Lawless Legends Developer Guide

## Version Management

Lawless Legends uses a centralized version management system to ensure consistent versioning across the application and packaging process.

### How to Update the Version Number

The version number is stored in a single file at the project root:

```
/version.properties
```

To update the version:

1. Edit the `version.properties` file in the project root
2. Update the `version` property with the new version number
3. Optionally, update the `build.date` property with the current date

Example:
```
version=1.1
build.date=2024-07-15
```

### How Version Information is Used

The version information is automatically used in:

1. **About Window**: The About dialog displays the version number from `version.properties`
2. **macOS Packaging**: The packaging script reads the version number from `version.properties` for DMG and app bundle versioning
3. **Application**: The `VersionInfo` utility class provides version information throughout the application

### Version Format Recommendations

For version numbering, we recommend following semantic versioning (MAJOR.MINOR.PATCH):

- **MAJOR**: Increment for incompatible API changes
- **MINOR**: Increment for backward-compatible new features
- **PATCH**: Increment for backward-compatible bug fixes

For example: `1.0.0`, `1.1.0`, `1.1.1`, etc.

#### macOS Packaging Compatibility

For macOS packaging compatibility, note the following:

- The macOS packaging system requires version numbers to be in the format of 1-3 integers separated by dots (e.g., `1.0`, `1.2.3`)
- If you use non-standard version formats (e.g., `1.0-beta.1`, `1.0-4n23k.99-DEMO`), the packaging script will automatically:
  - Extract only the part before the first hyphen for the application bundle version
  - Use the full version for the DMG file name and in the application's About window
- This allows you to use descriptive version strings while maintaining compatibility with macOS

For example, if `version.properties` contains `version=1.0-4n23k.99-DEMO`:
- The app bundle will use `1.0` as its internal version number
- The DMG file will be named `Lawless Legends 1.0-4n23k.99-DEMO.dmg`
- The About window will display `Version 1.0-4n23k.99-DEMO`

## Building the Application

To build the application:

```bash
mvn clean package
```

The version information is automatically included in the built JAR file.

## Packaging for Distribution

When packaging the application for distribution using the `package-macos.zsh` script, the version from `version.properties` is automatically used to:

1. Set the application bundle version
2. Name the DMG file with the appropriate version
3. Update internal version information in the app bundle's Info.plist

No additional configuration is needed to include version information when packaging.

## Development Guidelines

When adding new features that need version information:

1. Use the `VersionInfo` utility class to access version information:
   ```java
   import jace.core.VersionInfo;
   
   // Get the version string
   String version = VersionInfo.getVersion();
   
   // Get a formatted version display (includes build date if available)
   String versionDisplay = VersionInfo.getVersionDisplay();
   ```

2. Never hardcode version numbers in the application code 