#!/bin/bash
# The version of IdentityIQ; this is used to determine which zip file to extract and is used as a namespace separator
export IIQ_BASE_VERSION=8.3
# The base directory in which the IdentityIQ zip files are present
 
export BASE_SOFTWARE_PATH=/Users/bruce.ren/Desktop/tools-install/iiq-install/base
 
### DO NOT CHANGE ANYTHING BELOW THIS LINE
# ****************************************
# A working directory into which content is extracted; this is cleaned up later
export WORK_DIR=$BASE_SOFTWARE_PATH/iiqlibs
# Below are calculated from earlier values
export IIQ_ZIP_FILE=$BASE_SOFTWARE_PATH/identityiq-${IIQ_BASE_VERSION}.zip

export IIQ_VERSION=$IIQ_BASE_VERSION

export CURRENT_LOC=$PWD
echo "Current location: $CURRENT_LOC"

# Create the TMP_DIR, WORK_DIR
rm -rf $WORK_DIR && mkdir $WORK_DIR

# Extract to the WORK_DIR
if [ -f $IIQ_ZIP_FILE ]; then
  echo "Extracting identityiq.war from IIQ base GA"
  unzip -q -d $WORK_DIR/$IIQ_VERSION $IIQ_ZIP_FILE identityiq.war
  echo "Extracting the identity.war file"
  unzip -q -d $WORK_DIR/$IIQ_VERSION/identityiq $WORK_DIR/$IIQ_VERSION/identityiq.war
  echo "Renaming the identityiq.war file"
  mv $WORK_DIR/$IIQ_VERSION/identityiq.war $WORK_DIR/$IIQ_VERSION/iiq-webapp.war
  
else
  echo "No base GA file found; script will now terminate!"
  exit 1
fi

cd $CURRENT_LOC
# Upload each file in WEB-INF/lib to the repository
for filename in $WORK_DIR/$IIQ_VERSION/identityiq/WEB-INF/lib/*.jar; do
    fname=`basename $filename .jar`
    jarInfo=$(java -jar JarDependencyLookup-1.0.jar $filename $IIQ_VERSION)
    declare -a strarr="(${jarInfo//,/ })"
    groupId=${strarr[0]}
    artifactId=${strarr[1]}
    version=${strarr[2]}
    mvn install:install-file -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version -Dpackaging=jar -Dfile=$filename
done

# Add the identityiq.war file as a dependency for war file builds
mvn install:install-file -DgroupId=sailpoint -DartifactId=iiq-webapp -Dversion=$IIQ_VERSION -Dpackaging=war -Dfile=$WORK_DIR/$IIQ_VERSION/iiq-webapp.war


# Create a BOM file, the quick and dirty way
java -jar JarDependencyLookup-1.0.jar $WORK_DIR/$IIQ_VERSION/identityiq/WEB-INF/lib $IIQ_VERSION > $WORK_DIR/$IIQ_VERSION/pom.temp

# Pretty print and store the output
# cat $WORK_DIR/$IIQ_VERSION/pom.temp | xmllint --format - | tee $WORK_DIR/$IIQ_VERSION/pom.xml
cat $WORK_DIR/$IIQ_VERSION/pom.temp | tee $WORK_DIR/$IIQ_VERSION/pom.xml

# Upload the BOM pom.xml to the repo
mvn install:install-file -DgroupId=sailpoint -DartifactId=iiq-bom -Dversion=$IIQ_VERSION -Dpackaging=pom -Dfile=$WORK_DIR/$IIQ_VERSION/pom.xml

# Cleanup
rm -rf $WORK_DIR
