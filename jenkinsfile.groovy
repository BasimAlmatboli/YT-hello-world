pipeline {
    agent any
    triggers {
        // GitHub hook trigger for GITScm polling
        githubPush()
    }
    environment {
        AWS_REGION = 'eu-north-1'
        S3_BUCKET = 'test-project-for-jenkins'
        CODEBUILD_PROJECT = 'test'
    }
    
    stages {
        stage('Pull Code from GitHub') {
            steps {
                git branch: 'master', url: 'https://github.com/BasimAlmatboli/YT-hello-world.git'
            }
        }
        stage('Build on AWS CodeBuild') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: '99e62274-16c1-482c-b2e7-e575ee38fbb1') {
                    script {
                        def build = sh(script: "aws codebuild start-build --project-name ${CODEBUILD_PROJECT} --output json", returnStdout: true).trim()
                        echo "CodeBuild response: ${build}"
                    }
                }
            }
        }
        stage('Upload to S3') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: '99e62274-16c1-482c-b2e7-e575ee38fbb1') {
                    sh "aws s3 cp / s3://${S3_BUCKET}/ --recursive"
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline executed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
