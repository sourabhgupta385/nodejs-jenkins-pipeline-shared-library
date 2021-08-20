def call(String envName, String targetUrl, String configFilePath) {
    sh "artillery run -e ${envName} -t ${targetUrl} -o artillery-report.json ${configFilePath}"
    sh "artillery report -o artillery-report.html artillery-report.json"
    publishHTML target: [
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: './',
        reportFiles: 'artillery-report.html',
        reportName: 'Load Testing Report'
    ]
}