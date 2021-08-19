def call(String imageName, String imageTag, String dockerfilePath) {
    sh "buildah --storage-driver vfs bud -t ${imageName}:${imageTag} -f ${dockerfilePath}"
    sh "buildah push --storage-driver vfs localhost/${imageName}:${imageTag} docker-archive:${imageName}_${imageTag}.tar:${imageName}:${imageTag}"
}