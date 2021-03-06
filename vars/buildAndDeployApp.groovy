def call() {
    pipeline {
        agent none

        stages {
            stage('Read Properties') {
                agent any

                steps {
                    script {
                        properties = readYaml file: "properties.yaml"
                    }
                }
            }

            stage('Unit Testing & Code Coverage') {
                agent {
                    kubernetes {
                        yamlFile "${properties.NODEJS_SLAVE_YAML}"
                    }
                }

                steps {
                    container('nodejs') {
                        sh "npm install"
                        sh "npm run unit-test"
                        stash includes: 'coverage/*', name: 'coverage-report' 
                        stash includes: 'node_modules/', name: 'node_modules' 
                    }
                }    
            }

            stage('Security Testing'){
                parallel {
                    stage('Static Application Security Testing') {
                        agent {
                            kubernetes {
                                yamlFile "${properties.NODEJSSCAN_SLAVE_YAML}"
                            }
                        }
                        
                        steps {
                            container('nodejsscanner') {
                                sh "njsscan src --html -o 'nodejs-scanner-report.html' || true"
                                publishHTML target: [
                                    allowMissing: false,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: './',
                                    reportFiles: 'nodejs-scanner-report.html',
                                    reportName: 'SAST Report'
                                ]
                            }
                        }
                    }

                    stage('Software Composition Analysis') {
                        agent {
                            kubernetes {
                                yamlFile "${properties.OWASP_DEPENDENCY_CHECK_SLAVE_YAML}"
                            }
                        }
                        
                        steps {
                            container('owasp-dependency-checker') {
                                unstash 'node_modules'
                                sh "/usr/share/dependency-check/bin/dependency-check.sh --project 'DNVA' --scan ${properties.PACKAGE_JSON_PATH} --format ALL"
                                dependencyCheckPublisher pattern: "dependency-check-report.xml"
                                stash includes: "dependency-check-report.xml,dependency-check-report.json,dependency-check-report.html", name: 'owasp-reports' 
                            }
                        }
                    }
                }
            }

            stage('Code Quality Analysis') {
                agent {
                    kubernetes {
                        yamlFile "${properties.SONAR_SCANNER_SLAVE_YAML}"
                    }
                }
                steps {
                    container('sonar-scanner') {
                        withCredentials([usernamePassword(credentialsId: 'sonarqube-creds', usernameVariable: 'SONAR_USERNAME', passwordVariable: 'SONAR_PASSWORD')]) {
                            unstash 'coverage-report'
                            unstash 'owasp-reports'
                            sh "sonar-scanner -Dsonar.qualitygate.wait=true -Dsonar.host.url=${properties.SONAR_HOST_URL} -Dsonar.login=${SONAR_PASSWORD}"
                        }
                    }
                }
            }

            stage('Build Docker Image') {
                agent {
                    kubernetes {
                        yamlFile "${properties.BUILDAH_SLAVE_YAML}"
                    }
                }
                steps {
                    container('buildah') {
                        sh "buildah --storage-driver vfs bud -t ${properties.APP_NAME}:${BUILD_NUMBER} -f ${properties.DOCKERFILE_PATH}"
                        sh "buildah push --storage-driver vfs localhost/${properties.APP_NAME}:${BUILD_NUMBER} docker-archive:${properties.APP_NAME}_${BUILD_NUMBER}.tar:${properties.APP_NAME}:${BUILD_NUMBER}"
                        stash includes: "${properties.APP_NAME}_${BUILD_NUMBER}.tar", name: 'docker-image' 
                    }
                }
            }

            stage('Scan Docker Image') {
                agent {
                    kubernetes {
                        yamlFile "${properties.TRIVY_SLAVE_YAML}"
                    }
                }
                steps {
                    container('trivy-scanner') {
                        unstash 'docker-image'
                        sh "mkdir -p /tmp/trivy"
                        sh "chmod 754 /tmp/trivy"
                        // sh script: "TRIVY_NEW_JSON_SCHEMA=true trivy --cache-dir /tmp/trivy image --format json -o trivy-report.json --input ${properties.APP_NAME}_${BUILD_NUMBER}.tar"
                        sh script: "trivy --cache-dir /tmp/trivy image --format json -o trivy-report.json --input ${properties.APP_NAME}_${BUILD_NUMBER}.tar"
                        stash includes: 'trivy-report.json', name: 'trivy-report'                 
                    }
                }
            }

            stage('Push Docker Image') {
                agent {
                    kubernetes {
                        yamlFile "${properties.BUILDAH_SLAVE_YAML}"
                    }
                }
                steps {
                    container('buildah') {
                        withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_REGISTRY_USERNAME', passwordVariable: 'DOCKER_REGISTRY_PASSWORD')]) {
                            unstash 'docker-image'
                            sh "buildah login --log-level=debug -u ${DOCKER_REGISTRY_USERNAME} -p ${DOCKER_REGISTRY_PASSWORD} ${properties.DOCKER_REGISTRY_URL}"
                            sh "buildah pull docker-archive:${properties.APP_NAME}_${BUILD_NUMBER}.tar"
                            sh "buildah push localhost/${properties.APP_NAME}:${BUILD_NUMBER} docker://sourabh385/${properties.APP_NAME}:${BUILD_NUMBER}"
                        }    
                    }
                }
            }            

            stage('Deploy App in Staging') {
                agent any

                steps {
                    withKubeConfig([credentialsId: 'k8s-cluster-creds', serverUrl: "${properties.KUBERNETES_CLUSTER_URL}"]) {
                        sh "sed -i 's/IMAGE_NAME/${properties.APP_NAME}/g' ${properties.STAGING_KUSTOMIZATION_DIRECTORY}/kustomization.yaml"
                        sh "sed -i 's/IMAGE_TAG/${BUILD_NUMBER}/g' ${properties.STAGING_KUSTOMIZATION_DIRECTORY}/kustomization.yaml"
                        sh "kubectl -n ${properties.STAGING_NAMESPACE} apply -k ${properties.STAGING_KUSTOMIZATION_DIRECTORY}"
                    }
                }
            }

			stage('Functional Testing') {
                agent {
                    kubernetes {
                        yamlFile "${properties.NEWMAN_SLAVE_YAML}"
                    }
                }
                steps {
                    container('newman') {
                        sh "sed -i 's/APP_URL/${properties.APP_STAGING_TARGET_URL}/g' ${properties.POSTMAN_COLLECTION_FILE_PATH}"
                        sh "newman run ${properties.POSTMAN_COLLECTION_FILE_PATH} --verbose --reporters junit --reporter-junit-export='newman-report.xml'"
                        junit 'newman-report.xml'
                    }
                }
            }

            stage('Dynamic Application Security Testing') {
                agent {
                    kubernetes {
                        yamlFile "${properties.ZAP_SLAVE_YAML}"
                    }
                }
                steps {
                    container('zap') {
                        // It will exit with codes of:
                        // 	0:	Success
                        // 	1:	At least 1 FAIL
                        // 	2:	At least one WARN and no FAILs
                        // 	3:	Any other failure
                        sh "zap-full-scan.py -t http://${properties.APP_STAGING_TARGET_URL} -g gen.conf -r zap-report.html -x zap-report.xml || true"
                        sh "cp /zap/wrk/zap-report.html ./"
                        sh "cp /zap/wrk/zap-report.xml ./"
                        publishHTML target: [
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: './',
                            reportFiles: 'zap-report.html',
                            reportName: 'DAST Report'
                        ]      
                        stash includes: 'zap-report.xml', name: 'zap-report'                        
                    }
                }
            }

			stage('Load Testing') {
                agent {
                    kubernetes {
                        yamlFile "${properties.ARTILLERY_SLAVE_YAML}"
                    }
                }
                steps {
                    container('artillery') {
                        sh "artillery run -e staging -t http://${properties.APP_STAGING_TARGET_URL} -o artillery-report.json ${properties.ARTILLERY_CONFIG_FILE_PATH}"
                        sh "artillery report -o artillery-report.html artillery-report.json"
                        publishHTML target: [
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: './',
                            reportFiles: 'artillery-report.html',
                            reportName: 'Load Testing Report'
                        ]
                    }
                }
            }

            stage('Publish Reports to ArcherySec') {
                agent {
                    kubernetes {
                        yamlFile "${properties.ARCHERYSEC_SLAVE_YAML}"
                    }
                }
                steps {
                    container('archerysec-cli') {
                        withCredentials([usernamePassword(credentialsId: 'archerysec-creds', usernameVariable: 'ARCHERYSEC_USERNAME', passwordVariable: 'ARCHERYSEC_PASSWORD')]) {
                            unstash 'owasp-reports'
                            unstash 'trivy-report' 
                            unstash 'zap-report'
                        
                            sh "archerysec-cli -s ${properties.ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=XML --file=dependency-check-report.xml --TARGET=DVNA_OWASP --scanner=dependencycheck --project_id=${properties.ARCHERYSEC_PROJECT_ID}"

                            sh "archerysec-cli -s ${properties.ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=JSON --file=trivy-report.json --TARGET=DVNA_TRIVY --scanner=trivy --project_id=${properties.ARCHERYSEC_PROJECT_ID}"

                            sh "archerysec-cli -s ${properties.ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=XML --file=zap-report.xml --TARGET=DVNA_ZAP --scanner=zap_scan --project_id=${properties.ARCHERYSEC_PROJECT_ID}"

                            //sh "archerysec-cli -s ${properties.ARCHERYSEC_HOST_URL} -u ${ARCHERYSEC_USERNAME} -p ${ARCHERYSEC_PASSWORD} --upload --file_type=JSON --file=nodejs-scanner-report.json --TARGET=DVNA_NODEJSSCAN --scanner=nodejsscan --project_id=${properties.ARCHERYSEC_PROJECT_ID}"
                        }    
                    }
                }
            }

            stage('Deploy App in Production') {
                agent any

                steps {
                    withKubeConfig([credentialsId: 'k8s-cluster-creds', serverUrl: "${properties.KUBERNETES_CLUSTER_URL}"]) {
                        sh "sed -i 's/IMAGE_NAME/${properties.APP_NAME}/g' ${properties.PRODUCTION_KUSTOMIZATION_DIRECTORY}/kustomization.yaml"
                        sh "sed -i 's/IMAGE_TAG/${BUILD_NUMBER}/g' ${properties.PRODUCTION_KUSTOMIZATION_DIRECTORY}/kustomization.yaml"
                        sh "kubectl -n ${properties.PRODUCTION_NAMESPACE} apply -k ${properties.PRODUCTION_KUSTOMIZATION_DIRECTORY}"
                    }
                }
            }
        }
    }
}
