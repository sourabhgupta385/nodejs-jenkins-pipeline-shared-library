def call() {
    sh "npm install"
    sh "npm run unit-test"
}