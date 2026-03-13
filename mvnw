#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF)
# Maven Wrapper startup script for POSIX systems
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=`which java`
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

BASE_DIR=$(dirname "$(readlink -f "$0")" 2>/dev/null || pwd)
MAVEN_WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

DISTRIBUTION_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)

exec "$JAVACMD" \
  -classpath "$MAVEN_WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  "$@"
