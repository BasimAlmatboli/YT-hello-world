pipeline {
    agent any
    triggers {
        // GitHub hook trigger for GITScm polling
        githubPush()
    }
    
    environment {
        AWS_REGION = credentials('AWS_REGION')
        S3_BUCKET = credentials('S3_BUCKET')
        CODEBUILD_PROJECT = credentials('CODEBUILD_PROJECT')
    }
    
    stages {
        stage('Pull Code from GitHub') {
            steps {
                git branch: 'master', url: 'https://github.com/BasimAlmatboli/YT-hello-world.git'
            }
        }

        /*
         stage('Clean Up S3 Bucket') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: '10d1d59f-ace4-44ed-8496-7998a8f7f71a') {
                    echo "Cleaning up S3 bucket: ${S3_BUCKET}"
                    sh "aws s3 ls s3://${S3_BUCKET} --recursive"  // List contents before deletion for verification
                    sh "aws s3 rm s3://${S3_BUCKET} --recursive" // Delete all files in the S3 
                }
            }
        } */
        
        stage('Build with CodeBuild') {
            steps {
                script {
                    // Define variables
                    def projectName = "${CODEBUILD_PROJECT}"  // Use environment variable
                    def credentialsId = '99e62274-16c1-482c-b2e7-e575ee38fbb1'  //  CodeBuild credentials ID
                    def region = "${AWS_REGION}"  // Use environment variable
                    
                    // Start the AWS CodeBuild project
                    def result = awsCodeBuild(
                        projectName: projectName,
                        credentialsId: credentialsId,
                        region: region,
                        credentialsType: 'jenkins',  // 'jenkins' for Jenkins credentials
                        sourceControlType: 'project'  // 'project' for using the source of the Codebuild
                    )
                    
                    // Optional: Print build details
                    echo "Build ID: ${result.getBuildId()}"
                    echo "Build ARN: ${result.getArn()}"
                    echo "Artifacts Location: ${result.getArtifactsLocation()}"
                }
            }
        }

        stage('Check S3 Bucket') {
            steps {
                script {
                    def checkS3Files = sh(script: '''
                        #!/bin/bash
                        FILE_COUNT=$(aws s3 ls s3://${S3_BUCKET}/ --recursive | wc -l)
                        if [ $FILE_COUNT -gt 0 ]; then
                            echo "Files are uploaded to S3"
                            echo "Number of files: $FILE_COUNT"
                            exit 0
                        else
                            echo "Failed to Upload to S3"
                            exit 1
                        fi
                    ''', returnStatus: true)

                    if (checkS3Files != 0) {
                        error "Failed to Upload to S3"
                    }
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
