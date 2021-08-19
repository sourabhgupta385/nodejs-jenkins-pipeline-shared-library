def call(String namespace, String kustomizationDirectory) {
    sh "kubectl -n ${namespace} apply -k ${kustomizationDirectory}"
}