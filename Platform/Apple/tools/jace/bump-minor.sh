#!/bin/bash
# Automatically increment minor version (e.g., 3.5 -> 3.6)

CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version: $CURRENT_VERSION"

# Parse major.minor
MAJOR=$(echo $CURRENT_VERSION | cut -d. -f1)
MINOR=$(echo $CURRENT_VERSION | cut -d. -f2)

# Increment minor
NEW_MINOR=$((MINOR + 1))
NEW_VERSION="${MAJOR}.${NEW_MINOR}"

echo "New version: $NEW_VERSION"

# Update version in pom.xml
mvn versions:set -DnewVersion=$NEW_VERSION

echo "Version updated to $NEW_VERSION"
echo "Run 'git diff pom.xml' to verify changes"
