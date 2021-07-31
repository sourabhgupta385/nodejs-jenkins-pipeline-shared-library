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
            //     }
            // }

            stage('Code Quality Analysis') {
                agent {
                    kubernetes {
                        yamlFile 'k8s-manifests/sonar-scanner-pod.yaml'
                        defaultContainer 'jnlp'
                    }
                }
                steps {
                    container('sonar-scanner') {
                        git branch: 'main', url: 'https://github.com/sourabhgupta385/nodejs-ci-cd'
                        sh "sonar-scanner -Dsonar.qualitygate.wait=true"
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
