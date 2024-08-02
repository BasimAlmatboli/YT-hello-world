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
        
        stage('Build with CodeBuild') {
            steps {
                script {
                    // Define variables
                    def projectName = 'test'  // Replace with your CodeBuild project name
                    def credentialsId = '99e62274-16c1-482c-b2e7-e575ee38fbb1'  // Replace with your Jenkins credentials ID
                    def region = 'eu-north-1'  // Replace with your AWS region
                    
                    // Start the AWS CodeBuild project
                    def result = awsCodeBuild(
                        projectName: projectName,
                        credentialsId: credentialsId,
                        region: region,
                        credentialsType: 'jenkins',  // Use 'jenkins' for Jenkins credentials
                        sourceControlType: 'github'  // Ensure this matches your CodeBuild source control settings
                    )
                    
                    // Optional: Print build details
                    echo "Build ID: ${result.getBuildId()}"
                    echo "Build ARN: ${result.getArn()}"
                    echo "Artifacts Location: ${result.getArtifactsLocation()}"
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
