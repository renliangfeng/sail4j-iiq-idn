#!/bin/bash
# This script will install the following Maven artitects locally.
# These plugins assist in the build of an IdentityIQ implementation project.

# Original Author: Bruce Ren (bruce.ren@sailpoint.com)
# Version: 1.2
# Date: 25-May-2023

export VERSION=1.2

mvn install:install-file -DgroupId=com.sailpoint.sail4j -DartifactId=sail4j -Dversion=${VERSION} -Dpackaging=pom -Dfile=../sail4j-bundle/sail4j-${VERSION}.pom
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-api-${VERSION}.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-transform-${VERSION}.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-maven-plugin-${VERSION}.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-ant-task-${VERSION}.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-test-helper-${VERSION}.jar -DpomFile=../sail4j-bundle/sail4j-test-helper.pom.xml