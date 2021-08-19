def call(String imageName, String imageTag) {
    // sh script: "TRIVY_NEW_JSON_SCHEMA=true trivy --cache-dir /tmp/trivy image --format json -o trivy-report.json --input ${imageName}_${imageTag}.tar"
    sh script: "trivy --cache-dir /tmp/trivy image --format json -o trivy-report.json --input ${imageName}_${imageTag}.tar"
}