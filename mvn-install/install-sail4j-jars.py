from install_iiq_jars import (
    MavenInstallFileExecutor,
    FolderTraversalError,
    MissingRequiredDependencyError,
)
from pathlib import Path

VERSION = "1.3"
MAVEN_PLUGIN_INSTALL_COMMAND = [
    "mvn",
    "org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file",
]


class MavenPluginInstaller:
    file: str = None
    pom_file: str = None

    def __init__(self, file: str, pom_file: str = None):
        self.file = file
        self.pom_file = pom_file

    def get_maven_executor(self):
        executor = MavenInstallFileExecutor(MAVEN_PLUGIN_INSTALL_COMMAND)
        executor.file = self.file
        executor.pom_file = self.pom_file
        return executor


def run_maven(maven_executor: MavenInstallFileExecutor):
    print(
        f"\nExecuting: {maven_executor.execution_command} for {maven_executor.file}\n\n"
    )
    maven_executor.execute_command_without_output()
    print("\nExecution completed!")


def validate_file_path_is_correct(file_path: Path):
    if not (file_path.exists() and file_path.is_file()):
        raise MissingRequiredDependencyError(
            f"Unable to find dependency: {str(file_path)}"
        )


def get_sail4j_pom_executor(bundle_folder_path: Path):
    sail4j_pom_command = ["mvn", "install:install-file"]
    sail4j_pom_executor = MavenInstallFileExecutor(sail4j_pom_command)
    sail4j_pom_executor.artifact_id = "sail4j"
    sail4j_pom_executor.group_id = "com.sailpoint.sail4j"
    sail4j_pom_executor.version = VERSION
    sail4j_pom_executor.packaging = "pom"
    file_path = bundle_folder_path / f"sail4j-{VERSION}.pom"
    validate_file_path_is_correct(file_path)
    sail4j_pom_executor.file = str(file_path)
    return sail4j_pom_executor


def get_sail4j_api_executor(bundle_folder_path: Path):
    file_path = bundle_folder_path / f"sail4j-api-{VERSION}.jar"
    validate_file_path_is_correct(file_path)
    maven_plugin_installer = MavenPluginInstaller(str(file_path))
    return maven_plugin_installer.get_maven_executor()


def get_sail4j_transform_executor(bundle_folder_path: Path):
    file_path = bundle_folder_path / f"sail4j-transform-{VERSION}.jar"
    validate_file_path_is_correct(file_path)
    maven_plugin_installer = MavenPluginInstaller(str(file_path))
    return maven_plugin_installer.get_maven_executor()


def get_sail4j_maven_plugin_executor(bundle_folder_path: Path):
    file_path = bundle_folder_path / f"sail4j-maven-plugin-{VERSION}.jar"
    validate_file_path_is_correct(file_path)
    maven_plugin_installer = MavenPluginInstaller(str(file_path))
    return maven_plugin_installer.get_maven_executor()


def get_sail4j_ant_task_plugin_executor(bundle_folder_path: Path):
    file_path = bundle_folder_path / f"sail4j-ant-task-{VERSION}.jar"
    validate_file_path_is_correct(file_path)
    maven_plugin_installer = MavenPluginInstaller(str(file_path))
    return maven_plugin_installer.get_maven_executor()


def get_sail4j_test_helper_plugin_executor(bundle_folder_path: Path):
    file_path = bundle_folder_path / f"sail4j-test-helper-{VERSION}.jar"
    validate_file_path_is_correct(file_path)
    maven_plugin_installer = MavenPluginInstaller(str(file_path))
    pom_file_path = bundle_folder_path / "sail4j-test-helper.pom.xml"
    validate_file_path_is_correct(pom_file_path)
    maven_plugin_installer.pom_file = str(pom_file_path)
    return maven_plugin_installer.get_maven_executor()


def install_sail4j_dependencies(bundle_folder_path: Path):
    if not (bundle_folder_path.exists() and bundle_folder_path.is_dir()):
        raise FolderTraversalError(
            f"Unable to find the 'sail4j-bundle' folder using the path provided: {str(bundle_folder_path)}"
        )

    run_maven(get_sail4j_pom_executor(bundle_folder_path))
    run_maven(get_sail4j_api_executor(bundle_folder_path))
    run_maven(get_sail4j_transform_executor(bundle_folder_path))
    run_maven(get_sail4j_maven_plugin_executor(bundle_folder_path))
    run_maven(get_sail4j_ant_task_plugin_executor(bundle_folder_path))
    run_maven(get_sail4j_test_helper_plugin_executor(bundle_folder_path))


def main_execution():
    print("1. Please inform the full path for the 'sail4j-bundle' folder.")
    bundle_folder_path_str = input("sail4j-bundle path: ")
    bundle_folder_path = Path(bundle_folder_path_str)
    print("Installing dependencies..")
    install_sail4j_dependencies(bundle_folder_path)
    print("\n\nDependencies installed!")


if __name__ == "__main__":
    main_execution()
