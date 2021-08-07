def call(String agentLabel) {
    
    pipeline {
        agent none
        stages {
            // stage('Unit Test') {
            //     agent { label agentLabel }

            //     steps {
            //         sh "npm install"
            //         sh "npm run unit-test"
            //         stash includes: 'coverage/*', name: 'coverage-report' 
            //         stash includes: 'node_modules/', name: 'node_modules' 
            //     }
            // }

            // stage('Static Application Security Testing') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/nodejsscan-slave.yaml'
            //         }
            //     }
                
            //     steps {
            //         container('nodejsscanner') {
            //             sh "njsscan src --html -o 'nodejs-scanner-report.html' || true"
            //             sh "ls -ltr"
            //             publishHTML target: [
            //                 allowMissing: false,
            //                 alwaysLinkToLastBuild: true,
            //                 keepAll: true,
            //                 reportDir: './',
            //                 reportFiles: 'nodejs-scanner-report.html',
            //                 reportName: 'SAST Report'
            //             ]
            //         }
            //     }
            // }

            // stage('Software Composition Analysis') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/owasp-dependency-check-slave.yaml'
            //         }
            //     }
                
            //     steps {
            //         container('owasp-dependency-checker') {
            //             unstash 'node_modules'
            //             sh "/usr/share/dependency-check/bin/dependency-check.sh --project 'DNVA' --scan ./package.json --format ALL"
            //             dependencyCheckPublisher pattern: "dependency-check-report.xml"
            //             stash includes: "dependency-check-report.xml,dependency-check-report.json,dependency-check-report.html", name: 'owasp-reports' 
            //         }
            //     }
            // }

            // stage('Code Quality Analysis') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/sonar-scanner-slave.yaml'
            //         }
            //     }
            //     steps {
            //         container('sonar-scanner') {
            //             unstash 'coverage-report'
            //             unstash 'owasp-reports'
            //             sh "sonar-scanner -Dsonar.qualitygate.wait=true"
            //         }
            //     }
            // }

            stage('Build & Publish Docker Image') {
                agent {
                    kubernetes {
                        yamlFile 'k8s-manifests/slaves/buildah-slave.yaml'
                    }
                }
                steps {
                    container('buildah') {
                        //unstash 'node_modules'
                        sh "buildah --storage-driver vfs bud -t dvna-devsecops:${BUILD_NUMBER} -f Dockerfile"
                        sh "buildah push --storage-driver vfs localhost/dvna-devsecops:${BUILD_NUMBER} docker-archive:dvna_devsecops_${BUILD_NUMBER}.tar:dvna-devsecops:${BUILD_NUMBER}"
                        sh "ls -ltr"
                        //sh "buildah push --authfile '/tmp/config.json' --storage-driver vfs localhost/dvna-devsecops:${BUILD_NUMBER} docker://sourabh385/dvna-devsecops:${BUILD_NUMBER}"
                    }
                }
            }

            // stage('Deploy') {
            //     steps {
            //         sh 'pm2 restart all'
            //     }
            // }

            // stage('Integration Test') {
            //     steps {
            //         sh '''cd postman
            //                 newman run day01.postman_collection.json -r cli,junit'''
            //     }
            // }
        }
    }
}
