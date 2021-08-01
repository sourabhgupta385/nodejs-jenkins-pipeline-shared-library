def call(String agentLabel) {
    
    pipeline {
        agent none
        stages {
            // stage('Unit Test') {
            //     agent { label agentLabel }
            //     steps {
            //         // git branch: 'main', url: ''
            //         sh "npm install"
            //         sh "npm run unit-test"
            //         stash includes: 'coverage/*', name: 'coverage-report' 
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
                        sh "buildah images"
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
