#!/bin/bash
APP_HOME="$(cd "${BASH_SOURCE%/*}" && pwd)"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
CLASSPATH="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_HOME/bin/java" \
  "-Xmx2048m" \
  "-Dorg.gradle.appname=Gradle" \
  "-classpath" "$CLASSPATH" \
  "org.gradle.wrapper.GradleWrapperMain" \
  "$@"
