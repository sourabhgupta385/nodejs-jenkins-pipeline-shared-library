def call(String hostUrl, String username, String password, String fileType, String reportFileName, String target, String scanner, String projectId) {
    sh "archerysec-cli -s ${hostUrl} -u ${username} -p ${password} --upload --file_type=${fileType} --file=${reportFileName} --TARGET=${target} --scanner=${scanner} --project_id=${projectId}"
}