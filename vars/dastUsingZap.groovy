def call(String targetUrl) {
    // It will exit with codes of:
    // 	0:	Success
    // 	1:	At least 1 FAIL
    // 	2:	At least one WARN and no FAILs
    // 	3:	Any other failure
    sh "zap-full-scan.py -t ${targetUrl} -g gen.conf -r zap-report.html -x zap-report.xml || true"
    sh "cp /zap/wrk/zap-report.html ./"
    sh "cp /zap/wrk/zap-report.xml ./"
    publishHTML target: [
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: './',
        reportFiles: 'zap-report.html',
        reportName: 'DAST Report'
    ]
}