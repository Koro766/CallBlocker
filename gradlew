#!/bin/sh

# Gradle start-up script for POSIX systems
# Compatible with Linux / macOS / Android Termux

DIRNAME=""
CL_ARGS=""

APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
fi

# Locate wrapper jar
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -r "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper jar..."
    curl -sLo "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || \
    wget -qO "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || \
    echo "Please build with your system gradle: gradle assembleDebug"
fi

if [ -r "$WRAPPER_JAR" ]; then
    exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
elif command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Error: Neither Gradle wrapper nor system gradle found."
    echo "Please install OpenJDK and Gradle, or open this folder in Android Studio."
    exit 1
fi
