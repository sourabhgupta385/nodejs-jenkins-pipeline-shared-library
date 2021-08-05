def call(String agentLabel) {
    
    pipeline {
        agent none
        stages {
            stage('Unit Test') {
                agent { label agentLabel }

                steps {
                    sh "npm install"
                    sh "npm run unit-test"
                    //sh "npm audit"
                    stash includes: 'coverage/*', name: 'coverage-report' 
                    stash includes: 'node_modules/', name: 'node_modules' 
                }
            }

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

            // stage('Build & Publish Docker Image') {
            //     agent {
            //         kubernetes {
            //             yamlFile 'k8s-manifests/slaves/kaniko-slave.yaml'
            //         }
            //     }
            //     steps {
            //         container('kaniko') {
            //             sh "/kaniko/executor -f `pwd`/Dockerfile -c `pwd` --destination=sourabh385/nodejs-ci-cd:${BUILD_NUMBER}"
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
                        unstash 'node_modules'
                        sh "ls -ltr"
                        sh "buildah --storage-driver vfs bud -t dvna-devsecops:${BUILD_NUMBER} -f Dockerfile"
                        sh "buildah images --storage-driver vfs"
                        sh "buildah push --authfile '/tmp/config.json' --storage-driver vfs localhost/dvna-devsecops:${BUILD_NUMBER} docker://sourabh385/dvna-devsecops:${BUILD_NUMBER}"
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
