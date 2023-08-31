# The version of IdentityIQ; this is used to determine which zip file to extract and is used as a namespace separator
$IIQ_BASE_VERSION = "8.3"
# The base directory in which the IdentityIQ zip files are present
$BASE_SOFTWARE_PATH = "C:\Tools"
$JAR_LOOKUP_PATH = "$BASE_SOFTWARE_PATH\JarDependencyLookup-1.0.jar"

# DO NOT CHANGE ANYTHING BELOW THIS LINE
# ****************************************
# A working directory into which content is extracted; this is cleaned up later
$WORK_DIR = Join-Path -Path $BASE_SOFTWARE_PATH -ChildPath "iiqlibs"
# Below are calculated from earlier values
$IIQ_ZIP_FILE = Join-Path -Path $BASE_SOFTWARE_PATH -ChildPath "identityiq-$IIQ_BASE_VERSION.zip"

  
# Load the System.IO.Compression and System.IO.Compression.FileSystem assemblies
Add-Type -AssemblyName System.IO.Compression.FileSystem

$IIQ_VERSION = $IIQ_BASE_VERSION


# Create the TMP_DIR, WORK_DIR
Remove-Item -Path $WORK_DIR -Recurse -Force -ErrorAction SilentlyContinue
New-Item -Path $WORK_DIR -ItemType Directory | Out-Null

# Extract to the WORK_DIR
if (Test-Path $IIQ_ZIP_FILE) {
  Write-Host "Extracting identityiq.war from IIQ base GA"
  Expand-Archive -Path $IIQ_ZIP_FILE -DestinationPath "$WORK_DIR\$IIQ_VERSION" -Force
  Write-Host "Extracting the identity.war file"

  # Extract the .war file
  [System.IO.Compression.ZipFile]::ExtractToDirectory("$WORK_DIR\$IIQ_VERSION\identityiq.war", "$WORK_DIR\$IIQ_VERSION\identityiq")

 # Expand-Archive -Path "$WORK_DIR\$IIQ_VERSION\identityiq.war" -DestinationPath "$WORK_DIR\$IIQ_VERSION\identityiq" -Force
  Write-Host "Renaming the identityiq.war file"
  Rename-Item -Path "$WORK_DIR\$IIQ_VERSION\identityiq.war" -NewName "iiq-webapp.war"
} else {
  Write-Host "No base GA file found; script will now terminate!"
  Exit 1
}

Set-Location -Path "$WORK_DIR\$IIQ_VERSION\identityiq\WEB-INF\lib"

# Upload each file in WEB-INF/lib to the repository
Get-ChildItem -Path "$WORK_DIR\$IIQ_VERSION\identityiq\WEB-INF\lib" -Filter "*.jar" | ForEach-Object {
    $name = $_.Name
    $jarInfo = java -jar $JAR_LOOKUP_PATH $_.FullName $IIQ_VERSION
    $strarr = $jarInfo.Trim('()') -split ','
    $groupId = $strarr[0].Trim(' ')
    $artifactId = $strarr[1].Trim(' ')
    $version = $strarr[2].Trim(' ')

    mvn install:install-file "-DgroupId=$groupId" "-DartifactId=$artifactId" "-Dversion=$version" "-Dpackaging=jar" "-Dfile=$name"
}

# Add the iiq-webapp.war file as a dependency for war file builds
$WAR_FILE_PATH = "$WORK_DIR\$IIQ_VERSION\iiq-webapp.war"
mvn install:install-file "-DgroupId=sailpoint" "-DartifactId=iiq-webapp" "-Dversion=$IIQ_VERSION" "-Dpackaging=war" "-Dfile=$WAR_FILE_PATH" 

# Create a BOM file, the quick and dirty way
java -jar $JAR_LOOKUP_PATH "$WORK_DIR\$IIQ_VERSION\identityiq\WEB-INF\lib" $IIQ_VERSION |out-file "$WORK_DIR\$IIQ_VERSION\pom.tmp"
$POM_FILE_PATH = "$WORK_DIR\$IIQ_VERSION\pom.xml"
Get-Content -Path "$WORK_DIR\$IIQ_VERSION\pom.tmp" | Out-File -FilePath $POM_FILE_PATH -Encoding utf8

# Upload the BOM pom.xml to the repo
mvn install:install-file "-DgroupId=sailpoint" "-DartifactId=iiq-bom" "-Dversion=$IIQ_VERSION" "-Dpackaging=pom" "-Dfile=$POM_FILE_PATH"

# Cleanup
Set-Location $BASE_SOFTWARE_PATH
Remove-Item -Path $WORK_DIR -Recurse -Force
