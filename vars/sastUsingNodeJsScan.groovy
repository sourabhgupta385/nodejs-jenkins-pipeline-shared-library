def call() {
    sh "njsscan src --html -o 'nodejs-scanner-report.html' || true"
    publishHTML target: [
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: './',
        reportFiles: 'nodejs-scanner-report.html',
        reportName: 'SAST Report'
    ]
}