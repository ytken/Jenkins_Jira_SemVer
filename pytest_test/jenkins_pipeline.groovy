pipeline {
    agent any

    stages {
        stage('env-var') {
            steps {
                script {
                    env.GIT_HOME = "/app"
                }
            }
        }
        stage('Hello') {
            steps {
                script {
                    def commitHash = sh(returnStdout: true, script: "cd ${env.GIT_HOME} ; git rev-parse --short HEAD").trim()
                    def versionTag = sh(returnStdout: true, script: "cd ${env.GIT_HOME} ; git tag | tail -1").trim()
                    sh"""#!/bin/bash
                        shopt -s expand_aliases
                        
                        mkdir build_\${BUILD_NUMBER}
                        touch build_\${BUILD_NUMBER}/tests.log
                        touch build_\${BUILD_NUMBER}/commitHash.log
                        touch build_\${BUILD_NUMBER}/versionTag.log

                        echo ${commitHash} >> build_\${BUILD_NUMBER}/commitHash.log
                        echo ${versionTag} >> build_\${BUILD_NUMBER}/versionTag.log

                        pytest ${env.GIT_HOME}/test_capitalize.py 2>&1 | tee -a build_\${BUILD_NUMBER}/tests.log
                        status=\${PIPESTATUS[0]} # status of test_capitalize.py
                        
                        if [ \$status -ne 0 ]; then
                            echo 'ERROR: pytest failed, exiting ...'
                            exit \$status
                        fi
                    """
                }
            }
        }
    }
    post {
         regression {
            script {
                echo "Failure actions"
                
                echo "Diff between logs"
                
                def lastSuccesfulBuildId = sh(returnStdout: true, script: "cat ../../jobs/\$JOB_NAME/builds/permalinks | grep lastSuccessfulBuild | sed \'s/lastSuccessfulBuild //\'").trim()
                
                sh"""#!/bin/bash
                    shopt -s expand_aliases
                    diff -u build_${lastSuccesfulBuildId}/tests.log build_\${BUILD_NUMBER}/tests.log | tee build_diffs/\${BUILD_NUMBER}_${lastSuccesfulBuildId}_diff.log
                """
                def gitDiffLog = sh(returnStdout: true, script: "cat build_diffs/\${BUILD_NUMBER}_${lastSuccesfulBuildId}_diff.log").trim()
                
                echo "Diff between commits"
                def successCommit = sh(returnStdout: true, script: "head -n 1 build_${lastSuccesfulBuildId}/commitHash.log").trim()
                def failureCommit = sh(returnStdout: true, script: "head -n 1 build_\$BUILD_NUMBER/commitHash.log").trim()
                def gitDiffCommit = sh(returnStdout: true, script: "cd ${env.GIT_HOME} && git diff --submodule=diff ${successCommit} ${failureCommit}").trim()
                
                echo "Diff between versions"
                def successVersion = sh(returnStdout: true, script: "head -n 1 build_${lastSuccesfulBuildId}/versionTag.log").trim()
                def failureVersion = sh(returnStdout: true, script: "head -n 1 build_\$BUILD_NUMBER/versionTag.log").trim()
                def gitDiffVersion = sh(returnStdout: true, script: "cd ${env.GIT_HOME} ; cz changelog ${successVersion}..${failureVersion}").trim()

                def descriptionPath = "${JENKINS_HOME}/workspace/${env.JOB_BASE_NAME}/build_${BUILD_NUMBER}/description_file.txt"
                echo descriptionPath
                def issueDescription = """Произошло падение теста\nСсылка на запуск: ${RUN_DISPLAY_URL}\nЛог прохождения теста: ${JENKINS_HOME}/workspace/${env.JOB_BASE_NAME}/build_${BUILD_NUMBER}/tests.log\nРазличия между трассами прохождения успешного и упавшего запуска:\n{code}\n${gitDiffLog}\n{code}\nРазличия между успешным и ошибочных коммитами:\n{code}\n${gitDiffCommit}\n{code}\nВерсия текущего запуска: ${failureVersion}\nВерсия последнего успешного запуска: ${successVersion}\nChangelog: \n{code}\n${gitDiffVersion}\n{code}"""
                
                def descriptionFile = new File(descriptionPath)
                descriptionFile.createNewFile()
                descriptionFile.write(issueDescription)
                
                sh """
                    python3 ${env.GIT_HOME}/create_issue.py \"${env.JOB_BASE_NAME}\" \"a.ovchinnikova\" \"${descriptionPath}\" 
                """
            }
        }
    }
}
