## How to use Sail4j with Maven

### Install Apache Maven

Download Apache Maven 3.6.x or higher from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi). Installation is simply extracting the compressed archive, setting the M2_HOME environment variable and adding the $M2_HOME/bin to your $PATH. For example in Mac, just modify the file **.bash_profile** to add the Maven installation folder to PATH as below:

 		export M2_HOME=/Users/bruce.ren/Desktop/tools-install/apache-maven-3.6.3
		...
		export PATH=$PATH:$ANT_HOME/bin:$M2_HOME/bin
                                
### Install IdentityIQ and dependencies jars into local Maven repository
- Download the IdentityIQ base GA (e.g. **IdentityIQ 8.3.zip**) from Compass. The version of IIQ doesn't matter because it is only used to compile your java code which will be eventually converted to IDN Rules.

- Open the file **sail4j-iiq-idn/mvn-install/install-iiq-jars.sh** to modify the following 2 lines to match the version and location of IdentityIQ you download:

 		export IIQ_VERSION=8.3
 		export BASE_SOFTWARE_PATH=/Users/bruce.ren/Desktop/tools-install/iiq-install/base	            	
- Go to folder **sail4j-iiq-idn/mvn-install** to run the **install-iiq-jars.sh**, it takes a few minutes to install all the jar files.

		./install-iiq-jars.sh

### Install Sail4j jars into local Maven repository
- Modify the file **sail4j-iiq-idn/sail4j-bundle/sail4j-test-helper.pom.xml** to update the version of IdentityIQ configured in the previous step:

		<properties>
    		<IdentityIQ.Version>8.3</IdentityIQ.Version>
  		</properties>

- Go to folder **sail4j-iiq-idn/mvn-install** to run the **install-sail4j-jars.sh**.

		./install-sail4j-jars.sh


### Create Maven project
- Now you can create a Maven project by using the template project included in the following folder:

       sail4j-iiq-idn/maven-template

- Modify pom.xml to update groupId and artifactId if necessary. Please note this folder also includes 2 examples (with corresponding Junit test cases) for the reference.
- Import the project directory into Eclipse (or Intellij) as a Maven project.