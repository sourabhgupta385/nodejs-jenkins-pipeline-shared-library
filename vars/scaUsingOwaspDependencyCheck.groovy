def call(String projectName, String packageJsonPath) {
    sh "/usr/share/dependency-check/bin/dependency-check.sh --project ${projectName} --scan ${packageJsonPath} --format ALL"
    dependencyCheckPublisher pattern: "dependency-check-report.xml" 
}