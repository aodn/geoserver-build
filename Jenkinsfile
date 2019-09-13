pipeline {
    agent none

    stages {
        stage('clean') {
            agent { label 'master' }
            steps {
                sh 'git clean -fdx'
            }
        }

        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2'
                    additionalBuildArgs '--build-arg BUILDER_UID=$(id -u)'
                }
            }
            stages {
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
