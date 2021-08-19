def call() {
    sh "npm install"
    sh "npm run unit-test"
    stash includes: 'coverage/*', name: 'coverage-report' 
    stash includes: 'node_modules/', name: 'node_modules' 
}