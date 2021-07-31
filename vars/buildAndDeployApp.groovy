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
                        inheritFrom agentLabel
                        yaml '''
                        spec:
                          containers:
                            - name: jnlp
                              image: sonarsource/sonar-scanner-cli
                          imagePullSecret: image-registry-cred
                        '''
                    }
                }
                steps {
                    sh "sonar-scanner -Dsonar.qualitygate.wait=true"
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
