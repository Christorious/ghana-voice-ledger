#!/usr/bin/env sh

# Use this wrapper to download the Gradle wrapper jar if it does not exist
GRADLE_WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "Downloading Gradle wrapper jar..."
  mkdir -p gradle/wrapper
  curl -L -o "$GRADLE_WRAPPER_JAR" https://services.gradle.org/distributions/gradle-6.8.3-bin.zip
fi

exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
