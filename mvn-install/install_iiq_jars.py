import os
import shutil
from typing import Iterator
import zipfile
from pathlib import Path
import subprocess


class IdentityIQBaseGANotFoundError(Exception):
    """Raised when the Base GA file is not found."""

    pass


class MissingRequiredDependencyError(Exception):
    """Raised when we identified that Java or Maven are not correctly installed."""


class DependencyLookupFileNotFoundError(Exception):
    """Raised when the JarDependencyLookup-1.0.jar file is not found."""

    pass


class FolderTraversalError(Exception):
    """Raised when we are unable to reach a specific folder."""

    pass


class ExecutableJavaFileError(Exception):
    """Raised when we were unable to execute the Lookup Jar file."""

    pass


class JavaJarExecutor:
    execution_arguments: list[str]
    execution_full_command: list[str]
    file_name: str

    def __init__(
        self, execution_args: list[str], execution_command: list[str], file_name: str
    ):
        self.execution_arguments = execution_args
        self.execution_full_command = execution_command
        self.file_name = file_name

    def execute_command_with_output(self):
        executable = self.execution_full_command + self.execution_arguments
        return subprocess.run(executable, capture_output=True, text=True, check=True)

    def execute_command_without_output(self):
        executable = self.execution_full_command + self.execution_arguments
        return subprocess.run(executable)


class MavenInstallFileExecutor:
    group_id: str = None
    artifact_id: str = None
    version: str = None
    packaging: str = None
    file: str = None
    pom_file: str = None
    execution_command: list[str] = None

    def __init__(self, command: list[str]):
        self.execution_command = command

    def get_execution_arguments(self):
        if (
            self.group_id is None
            and self.artifact_id is None
            and self.version is None
            and self.packaging is None
        ):
            if self.pom_file is not None:
                return [f"-Dfile={self.file}", f"-DpomFile={self.pom_file}"]
            else:
                return [f"-Dfile={self.file}"]

        return [
            f"-DgroupId={self.group_id}",
            f"-DartifactId={self.artifact_id}",
            f"-Dversion={self.version}",
            f"-Dpackaging={self.packaging}",
            f"-Dfile={self.file}",
        ]

    def execute_command_with_output(self):
        executable = self.execution_command + self.get_execution_arguments()
        return subprocess.run(
            executable, capture_output=True, text=True, check=True, shell=True
        )

    def execute_command_without_output(self):
        executable = self.execution_command + self.get_execution_arguments()
        return subprocess.run(executable, shell=True)


def is_valid_file(file_path: Path):
    return file_path.exists() and file_path.is_file()


def is_valid_dir(dir_path: Path):
    return dir_path.exists() and dir_path.is_dir()


def delete_work_dir(work_dir_path: Path):
    print("Checking if the WORK DIR exists.")

    if is_valid_dir(work_dir_path):
        print(f"Detected WORK_DIR. Removing it : {str(work_dir_path)}")
        shutil.rmtree(work_dir_path)
        print("WORK_DIR removed")


def create_new_work_dir(work_dir_path: Path):
    print("Creating new WORK_DIR")
    work_dir_path.mkdir(parents=True, exist_ok=True)


def extract_iiq_zip_file(iiq_zip_file_path: Path, iiq_folder: Path):
    print("Extracting The IdentityIQ zip file")
    shutil.unpack_archive(iiq_zip_file_path, extract_dir=iiq_folder)
    print("File extracted")


def extract_iiq_war_file(iiq_folder: Path, war_file_path: Path):
    print("Extracting identityiq.war")

    with zipfile.ZipFile(war_file_path, "r") as war_archive:
        war_archive.extractall(iiq_folder)

    print("War File extracted")


def rename_iiq_war_file(iiq_folder: Path, war_file_path: Path):
    print("Renaming the identityiq.war file")
    new_war_file_path = iiq_folder / "iiq-webapp.war"
    war_file_path.rename(new_war_file_path)
    print("File renamed")
    return new_war_file_path


def validate_webinf_lib_folder(webinf_lib_folder_path: Path):
    print(f"Checking if {str(webinf_lib_folder_path)} exists")

    if not is_valid_dir(webinf_lib_folder_path):
        raise FolderTraversalError(f"Invalid dir: {str(webinf_lib_folder_path)}")


def change_to_webinf_lib_folder(webinf_lib_folder_path: Path):
    print("Changing current dir")
    os.chdir(webinf_lib_folder_path)
    print("Changed dir")


def validate_iiq_zip_file(iiq_zip_file_path: Path, iiq_base_version: str):
    print("\n\nValidating if the IIQ zip file is present.")

    if not is_valid_file(iiq_zip_file_path):
        print("No base GA file found; script will now terminate!")
        raise IdentityIQBaseGANotFoundError(
            f"No base GA file found for this version: {iiq_base_version}"
        )


def validate_required_jar_dependency(jar_dependency_lookup_path: Path):
    print("Validating if the required dependency is present.")

    if not is_valid_file(jar_dependency_lookup_path):
        print("Unable to find a required dependency: JarDependencyLookup-1.0.jar")
        raise DependencyLookupFileNotFoundError(
            f"Unable to find a required dependency: {str(jar_dependency_lookup_path)}"
        )


def execute_maven_install(maven_executors: list[MavenInstallFileExecutor]):
    print(
        f"Starting execution of Maven Install commands for {len(maven_executors)} dependencies"
    )
    print("This will run the mvn install:install-file for them")

    for maven_executor in maven_executors:
        try:
            maven_executor.execute_command_without_output()
        except subprocess.CalledProcessError as call_error:
            raise ExecutableJavaFileError(
                f"Unable to execute Maven command {maven_executor.execution_command}",
                call_error,
            )

    print("Finished execution of all Maven commands!")


def get_java_executors(
    jar_files_iterator: Iterator[Path], iiq_version: str, jar_lookup_path_str: str
):
    java_executors = []

    for jar_file in jar_files_iterator:
        if jar_file.is_file():
            file_name = jar_file.name
            full_path = str(jar_file)
            executable_args = [full_path, iiq_version]
            command_to_execute = ["java", "-jar", jar_lookup_path_str]
            java_executor = JavaJarExecutor(
                executable_args, command_to_execute, file_name
            )
            java_executors.append(java_executor)

    return java_executors


def get_maven_install_commands(java_executor_list: list[JavaJarExecutor]):
    print(
        f"Starting execution of Java commands for {len(java_executor_list)} dependencies"
    )
    print(
        "This will run the JarDependencyLookup-1.0 for each dependency stored in WEB-INF/lib"
    )
    maven_commands_to_run = []

    for java_executor in java_executor_list:
        try:
            result = java_executor.execute_command_with_output()
            output_text = result.stdout

            if not output_text:
                continue

            output_groups = (
                output_text.replace("(", "").replace(")", "").strip().split(",")
            )

            if not (output_groups is not None and len(output_groups) > 2):
                continue

            maven_execution_group_id = output_groups[0]
            maven_execution_artifact_id = output_groups[1]
            maven_execution_version = output_groups[2]

            if not (
                maven_execution_group_id
                and maven_execution_artifact_id
                and maven_execution_version
            ):
                continue

            maven_execution_group_id = maven_execution_group_id.strip()
            maven_execution_artifact_id = maven_execution_artifact_id.strip()
            maven_execution_version = maven_execution_version.strip()
            maven_execution_command = ["mvn", "install:install-file"]

            maven_executor = MavenInstallFileExecutor(maven_execution_command)
            maven_executor.group_id = maven_execution_group_id
            maven_executor.artifact_id = maven_execution_artifact_id
            maven_executor.version = maven_execution_version
            maven_executor.file = java_executor.file_name
            maven_executor.packaging = "jar"
            maven_commands_to_run.append(maven_executor)
        except subprocess.CalledProcessError as call_error:
            raise ExecutableJavaFileError("Unable to run Java command!", call_error)

    print("Finished execution of all Java commands!")
    return maven_commands_to_run


def install_maven_dependencies_from_iiq(
    lib_folder: Path, iiq_version: str, jar_lookup_path_str: str
):
    print("Installing IIQ dependencies to Sail4j")
    jar_files_iterator = lib_folder.rglob("*.jar")
    java_executors = get_java_executors(
        jar_files_iterator, iiq_version, jar_lookup_path_str
    )
    maven_executors = get_maven_install_commands(java_executors)
    execute_maven_install(maven_executors)
    print("Dependencies installed")


def include_war_file_as_dependency_for_war_builds(
    iiq_version: str, new_war_file_path: Path
):
    iiq_webapp_war_command = ["mvn", "install:install-file"]
    maven_executor = MavenInstallFileExecutor(iiq_webapp_war_command)
    maven_executor.group_id = "sailpoint"
    maven_executor.artifact_id = "iiq-webapp"
    maven_executor.version = iiq_version
    maven_executor.packaging = "war"
    maven_executor.file = new_war_file_path

    try:
        maven_executor.execute_command_without_output()
    except subprocess.CalledProcessError as error_to_run_maven:
        raise ExecutableJavaFileError(
            f"Execution of {iiq_webapp_war_command} has failed!", error_to_run_maven
        )

    print(f"Executed {iiq_webapp_war_command}")


def generate_bom_file(
    webinf_lib_folder_path: Path,
    iiq_version: str,
    workdir_with_iiq_path: Path,
    jar_dependency_lookup_path_str: str,
):
    bom_file_creation_arguments = [str(webinf_lib_folder_path), iiq_version]

    bom_file_creation_command = ["java", "-jar", jar_dependency_lookup_path_str]
    java_executor = JavaJarExecutor(
        bom_file_creation_arguments, bom_file_creation_command, None
    )
    print(
        f"Executing command {bom_file_creation_command} with args {bom_file_creation_arguments}"
    )

    try:
        bom_file_result = java_executor.execute_command_with_output()
        pom_xml = bom_file_result.stdout
        permanent_bom_file_path = Path(workdir_with_iiq_path) / "pom.xml"

        with open(permanent_bom_file_path, "w", encoding="utf-8") as pom_permanent_file:
            pom_permanent_file.write(pom_xml)

        print("Finished writing to the pom.xml file")
        maven_install_pom_file_command = ["mvn", "install:install-file"]

        maven_executor = MavenInstallFileExecutor(maven_install_pom_file_command)
        maven_executor.group_id = "sailpoint"
        maven_executor.artifact_id = "iiq-bom"
        maven_executor.version = iiq_version
        maven_executor.packaging = "pom"
        maven_executor.file = str(permanent_bom_file_path)

        # Upload the BOM pom.xml to the repo
        print(f"Executing {maven_install_pom_file_command}")

        try:
            maven_executor.execute_command_without_output()
        except subprocess.CalledProcessError as error_to_install_pom_xml_file:
            raise ExecutableJavaFileError(
                f"Execution of {maven_install_pom_file_command} has failed!",
                error_to_install_pom_xml_file,
            )

        print(f"Executed {maven_install_pom_file_command}")

    except subprocess.CalledProcessError as error_to_create_temp_bom_file:
        raise ExecutableJavaFileError(
            f"Execution of {bom_file_creation_command} has failed!",
            error_to_create_temp_bom_file,
        )


def perform_cleanup(base_path: Path, work_dir: Path):
    print(f"Changing to {str(base_path)} and removing the folder {str(work_dir)}")
    os.chdir(base_path)
    shutil.rmtree(work_dir)
    print("Done!")


def upload_pom_xml_file_to_repository(work_dir: Path, iiq_version: str):
    print("Uploading pom.xml file with the generated bom")
    pom_file_path = Path(work_dir) / iiq_version / "pom.xml"

    if not is_valid_file(pom_file_path):
        raise FolderTraversalError("Could not localize the pom.xml file!")

    maven_command = ["mvn", "install:install-file"]
    maven_executor = MavenInstallFileExecutor(maven_command)
    maven_executor.artifact_id = "iiq-bom"
    maven_executor.group_id = "sailpoint"
    maven_executor.version = iiq_version
    maven_executor.packaging = "pom"
    maven_executor.file = str(pom_file_path)

    try:
        maven_executor.execute_command_without_output()
    except subprocess.CalledProcessError as execution_error:
        raise ExecutableJavaFileError(
            "Unable to upload the pom.xml file with the bom!", execution_error
        )

    print("pom.xml file successfully uploaded!")


def validate_java_installation():
    print("Checking if you have Java installed")
    if shutil.which("java") is None:
        raise MissingRequiredDependencyError(
            "Java is not installed! Unable to continue"
        )


def validate_maven_installation():
    print("Checking if you have Maven installed")
    if shutil.which("mvn") is None:
        raise MissingRequiredDependencyError(
            "Apache Maven is not installed! Unable to continue"
        )


def main_execution():
    print("Started the script execution.")

    validate_java_installation()
    validate_maven_installation()

    print("1. Please inform the SailPoint IdentityIQ Version that you've installed.")
    print(
        "2. Please inform the BASE PATH where the 'JarDependencyLookup-1.0.jar' and the identityiq base GA (zip file) are installed."
    )
    print("2.1. NOTE: Type 'current' to use the current working directory.\n")

    iiq_base_version = input("IdentityIQ Version: ")
    base_software_path = input("Working Directory: ")

    if base_software_path == "current":
        base_software_path = os.getcwd()

    base_path = Path(base_software_path)
    work_dir = Path(base_path) / "iiqlibs"
    work_dir_with_iiq = work_dir / iiq_base_version
    iiq_folder = work_dir_with_iiq / "identityiq"
    war_file_path = iiq_folder / "identityiq.war"
    jar_dependency_lookup_path = base_path / "JarDependencyLookup-1.0.jar"
    jar_dependency_lookup_path_str = str(jar_dependency_lookup_path)
    iiq_zip_file = base_path / f"identityiq-{iiq_base_version}.zip"
    webinf_lib_folder_path = iiq_folder / "WEB-INF" / "lib"

    validate_iiq_zip_file(iiq_zip_file, iiq_base_version)
    validate_required_jar_dependency(jar_dependency_lookup_path)
    delete_work_dir(work_dir)
    create_new_work_dir(work_dir)
    extract_iiq_zip_file(iiq_zip_file, iiq_folder)
    extract_iiq_war_file(iiq_folder, war_file_path)
    new_war_file_path = rename_iiq_war_file(iiq_folder, war_file_path)
    iiq_folder = work_dir_with_iiq / "identityiq"
    webinf_lib_folder_path = iiq_folder / "WEB-INF" / "lib"
    validate_webinf_lib_folder(webinf_lib_folder_path)
    change_to_webinf_lib_folder(webinf_lib_folder_path)
    install_maven_dependencies_from_iiq(
        webinf_lib_folder_path, iiq_base_version, jar_dependency_lookup_path_str
    )
    include_war_file_as_dependency_for_war_builds(iiq_base_version, new_war_file_path)
    generate_bom_file(
        webinf_lib_folder_path,
        iiq_base_version,
        work_dir_with_iiq,
        jar_dependency_lookup_path_str,
    )
    upload_pom_xml_file_to_repository(work_dir, iiq_base_version)
    perform_cleanup(base_path, work_dir)


if __name__ == "__main__":
    main_execution()
