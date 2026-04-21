#!/bin/sh
# Maven Wrapper script — downloads Maven 3.9.9 on first run.
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See NOTICE and LICENSE for details.

set -e

# OS specific support.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  Darwin*)  darwin=true ;;
  MINGW*)   mingw=true ;;
esac

if [ -z "$JAVA_HOME" ]; then
  if $darwin; then
    JAVA_HOME=$(/usr/libexec/java_home)
  else
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(command -v java))))
  fi
fi

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

# Read properties
DISTRIBUTION_URL=$(grep "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/^distributionUrl=//')
WRAPPER_JAR_URL=$(grep "^wrapperUrl=" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/^wrapperUrl=//')

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  if command -v curl > /dev/null 2>&1; then
    curl -sL "$WRAPPER_JAR_URL" -o "$MAVEN_WRAPPER_JAR"
  elif command -v wget > /dev/null 2>&1; then
    wget -q "$WRAPPER_JAR_URL" -O "$MAVEN_WRAPPER_JAR"
  else
    echo "ERROR: Cannot download maven-wrapper.jar (neither curl nor wget available)." >&2
    exit 1
  fi
fi

exec "$JAVA_HOME/bin/java" \
  ${MAVEN_OPTS} \
  -classpath "$MAVEN_WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=${PWD}" \
  "org.apache.maven.wrapper.MavenWrapperMain" \
  "$@"
