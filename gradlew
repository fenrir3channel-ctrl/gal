#!/bin/sh

GRADLE_VERSION="8.2"
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-8.2"

if [ ! -d "$GRADLE_HOME" ]; then
    echo "Gradle not found. Please install manually."
    exit 1
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
