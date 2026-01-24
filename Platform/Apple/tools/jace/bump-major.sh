#!/bin/bash
# Automatically increment major version (e.g., 3.5 -> 4.0)

CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version: $CURRENT_VERSION"

# Parse major.minor
MAJOR=$(echo $CURRENT_VERSION | cut -d. -f1)

# Increment major, reset minor to 0
NEW_MAJOR=$((MAJOR + 1))
NEW_VERSION="${NEW_MAJOR}.0"

echo "New version: $NEW_VERSION"

# Update version in pom.xml
mvn versions:set -DnewVersion=$NEW_VERSION

echo "Version updated to $NEW_VERSION"
echo "Run 'git diff pom.xml' to verify changes"
