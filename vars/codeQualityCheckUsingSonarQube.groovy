def call(String hostUrl, String password) {
    sh "sonar-scanner -Dsonar.qualitygate.wait=true -Dsonar.host.url=${hostUrl} -Dsonar.login=${password}"
}