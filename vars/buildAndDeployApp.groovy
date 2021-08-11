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
            //             sh "njsscan src --json -o 'nodejs-scanner-report.json' || true"
            //             sh "ls -ltr"
            //             stash includes: 'nodejs-scanner-report.json', name: 'nodejs-scanner-report' 
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

            // stage('Build Docker Image') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/buildah-slave.yaml'
            //         }
            //     }
            //     steps {
            //         container('buildah') {
            //             sh "buildah --storage-driver vfs bud -t dvna-devsecops:${BUILD_NUMBER} -f Dockerfile"
            //             sh "buildah push --storage-driver vfs localhost/dvna-devsecops:${BUILD_NUMBER} docker-archive:dvna_devsecops_${BUILD_NUMBER}.tar:dvna-devsecops:${BUILD_NUMBER}"
            //             stash includes: "dvna_devsecops_${BUILD_NUMBER}.tar", name: 'docker-image' 
            //         }
            //     }
            // }

            // stage('Scan Docker Image') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/trivy-slave.yaml'
            //         }
            //     }
            //     steps {
            //         container('trivy-scanner') {
            //             unstash 'docker-image'
            //             sh "mkdir -p /tmp/trivy"
            //             sh "chmod 754 /tmp/trivy"
            //             sh script: 'TRIVY_NEW_JSON_SCHEMA=true trivy --cache-dir /tmp/trivy image --format json -o trivy-report.json --input dvna_devsecops_${BUILD_NUMBER}.tar'  
            //             stash includes: 'trivy-report.json', name: 'trivy-report'                 
            //         }
            //     }
            // }

            // stage('Push Docker Image') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/buildah-slave.yaml'
            //         }
            //     }
            //     steps {
            //         container('buildah') {
            //             unstash 'docker-image'
            //             sh "buildah pull docker-archive:dvna_devsecops_${BUILD_NUMBER}.tar"
            //             sh "buildah push --authfile '/tmp/config.json' localhost/dvna-devsecops:${BUILD_NUMBER} docker://sourabh385/dvna-devsecops:${BUILD_NUMBER}"
            //         }
            //     }
            // } 

            stage('Publish Reports to ArcherySec') {
                agent {
                    kubernetes {
                        yamlFile 'k8s-manifests/slaves/archerysec-slave.yaml'
                    }
                }
                steps {
                    container('archerysec-cli') {
                        // unstash 'nodejs-scanner-report'
                        // unstash 'owasp-reports'
                        // unstash 'trivy-report' 

                        // sh "archerysec-cli -s ${ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=JSON --file=nodejs-scanner-report.json --TARGET=test --scanner=nodejsscanner --project_id=81632946-09b6-446c-aded-699a702563da"
                        sh "printenv"
                        sh "echo ${env.ARCHERYSEC_HOST_URL}"
                        sh "archerysec-cli -s ${ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=XML --file=dependency-check-report.xml --TARGET=test --scanner=dependencycheck --project_id=81632946-09b6-446c-aded-699a702563da"

                        sh "archerysec-cli -s ${ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=JSON --file=trivy-report.json --TARGET=test --scanner=trivy --project_id=81632946-09b6-446c-aded-699a702563da"
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
