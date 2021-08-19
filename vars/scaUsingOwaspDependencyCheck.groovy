def call() {
    sh "/usr/share/dependency-check/bin/dependency-check.sh --project 'DNVA' --scan ${properties.PACKAGE_JSON_PATH} --format ALL"
    dependencyCheckPublisher pattern: "dependency-check-report.xml" 
}