def call(Map configMap){
    pipeline{
    agent{
        node{
            label 'AGENT-1'
        }
    }
    environment{
        PackageVersion=''
        
    }
    options{
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    parameters{
        booleanParam(name:'deploy', defaultValue:false, description:'toggle this value')
    }
    stages{
        stage('get the version'){
            steps{
                script{
                    def packagejson = readJSON file: 'package.json'
                    PackageVersion = packagejson.version
                    echo "application version:$PackageVersion"
                }
            }
        }
        stage('install dependancies'){
            steps{
                sh """
                  npm install
                """
            }
        }
        stage('uint tests'){
            steps{
                sh """
                  echo "unit tests will run here"
                """
            }
        }
        stage('sonar scan'){
            steps{
                sh """
                  echo "sonar scan will run here"
                """
            }
        }
        stage('build'){
            steps{
                sh """
                  ls -la
                  zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                  ls -ltr
                """
            }
        }
        stage('publish artifact'){
            steps{
              nexusArtifactUploader(
                nexusVersion: 'nexus3',
                protocol: 'http',
                nexusUrl: pipelineGlobal.nexusURL(),
                groupId: 'com.roboshop',
                version: "${PackageVersion}",
                repository: "${configMap.component}",
                credentialsId: 'nexus-auth',
                artifacts: [
                 [artifactId: "${configMap.component}" ,
                  classifier: '',
                  file: "${configMap.component}".zip,
                  type: 'zip']
                ]
             )
            }
        }
        stage('deploy'){
            when{
                expression{
                    params.Deploy=='true'
                }
            }
            steps{
                script{
                    def params = [
                        string(name: 'version', value: "$PackageVersion"),
                        string(name: 'environment', value: "dev")
                        ]
                    build job: "../${configMap.component}-deploy", wait:true, parameters: params
                }
            }
        }
    }
    post{
        always{
            echo "i will always say hello again"
            deleteDir()
        }
        success{
            echo "i will always say hello when pipeline is success"
        }
        failure{
            echo "this is when pipeline failed"
        }
    }
}
}