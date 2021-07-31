def call(String agentLabel) {
    
    pipeline {
        agent { label: agentLabel }

        stages {
            stage('Checkout') {
                steps {
                    // git branch: 'main', url: ''
                    sh "ls -ltr"
                }
            }

            // stage('Unit Test') {
            //     steps {
            //         sh "npm install"
            //         sh "npm test"
            //     }
            // }

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
