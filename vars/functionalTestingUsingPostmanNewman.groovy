def call(String postmanCollectionFilePath) {
    sh "newman run ${postmanCollectionFilePath} --verbose --reporters junit --reporter-junit-export='newman-report.xml'"
}