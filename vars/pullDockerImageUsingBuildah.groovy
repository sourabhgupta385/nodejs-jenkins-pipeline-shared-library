def call(String registryUsername, String registryPassword, String registryUrl, String imageName, String imageTag) {
    sh "buildah login --log-level=debug -u ${registryUsername} -p ${registryPassword} ${registryUrl}"
    sh "buildah pull docker-archive:${imageName}_${imageTag}.tar"
}