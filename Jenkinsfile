pipeline {
    agent none

    stages {
        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2 -v ${HOME}/bin:${HOME}/bin'
                    additionalBuildArgs '--build-arg BUILDER_UID=$(id -u)'
                }
            }
            stages {
                stage('clean') {
                    steps {
                        sh 'git reset --hard'
                        sh 'git clean -xffd'
                    }
                }
                stage('set_version_release') {
                    when { branch "master" }
                    steps {
                        withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIALS_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh './bumpversion.sh'
                        }
                    }
                }
//                 stage('set_codeartifact_token') {
//                     steps {
//                         sh 'export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain cmrose-maven --domain-owner 615645230945 --region ap-southeast-2 --query authorizationToken --output text`'
//                     }
//                 }
                stage('install_local_dependencies') {
                    steps {
                        sh 'mvn install:install-file -Dfile=gs-gwc-s3.jar -DgroupId=org.geoserver.community -DartifactId=gs-gwc-s3 -Dversion=2.23.0 -Dpackaging=jar'
                    }
                }
                stage('build') {
                    steps {
                        sh 'mvn -B -DskipTests clean compile -s settings.xml'
                    }
                }
                stage('test') {
                    steps {
                        sh 'mvn -B test'
                    }
                }
                stage('package') {
                    steps {
                        sh 'mvn -B -DskipTests package'
                    }
                }
            }
            post {
                success {
                    dir('src/main/target/') {
                        archiveArtifacts artifacts: '*.war', fingerprint: true, onlyIfSuccessful: true
                    }
                }
            }
        }
    }
}
