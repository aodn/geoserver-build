pipeline {
    agent { label 'master' }

    stages {
        stage('clean') {
            steps {
                sh 'git clean -fdx'
            }
        }
        stage('set_version') {
            steps {
                sh 'bumpversion patch'
            }
        }
        stage('release') {
            when { branch 'master' }
            steps {
                sh 'bumpversion --tag --commit --allow-dirty release'
            }
        }
        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2'
                    additionalBuildArgs '--build-arg BUILDER_UID=${JENKINS_UID:-9999}'
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
