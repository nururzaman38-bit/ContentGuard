#!/bin/sh
# Minimal checked-in wrapper launcher; fetches the tiny wrapper bootstrap on first CI run.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  command -v curl >/dev/null 2>&1 || { echo "curl is required to bootstrap Gradle" >&2; exit 1; }
  mkdir -p "$(dirname "$JAR")"
  curl --fail --location --retry 3 -o "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar || exit 1
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
