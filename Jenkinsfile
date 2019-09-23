pipeline {
    agent none

    stages {
        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2'
                    additionalBuildArgs '--build-arg BUILDER_UID=$(id -u)'
                }
            }
            stages {
                stage('set_version_build') {
                    when { not { branch "master" } }
                    steps {
                        sh './bumpversion.sh build'
                    }
                }
                stage('set_version_release') {
                    when { branch "master" }
                    steps {
                        sh './bumpversion.sh release'
                    }
                }
                stage('build') {
                    steps {
                        sh 'mvn -B -DskipTests clean compile'
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
                    archiveArtifacts artifacts: 'src/main/target/*.war', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }
    }
}
