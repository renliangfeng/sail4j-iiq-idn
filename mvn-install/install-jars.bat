@echo off

set "VERSION=1.1"

mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=identityiq.jar -DpomFile=identityiq.pom.xml

mvn install:install-file -DgroupId=com.sailpoint.sail4j -DartifactId=sail4j -Dversion=%VERSION% -Dpackaging=pom -Dfile=../sail4j-bundle/sail4j-%VERSION%.pom
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-api-%VERSION%.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-transform-%VERSION%.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-maven-plugin-%VERSION%.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-ant-task-%VERSION%.jar
mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=../sail4j-bundle/sail4j-test-helper-%VERSION%.jar -DpomFile=../sail4j-bundle/sail4j-test-helper.pom.xml