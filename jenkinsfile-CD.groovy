pipeline {
    agent any

    environment {
        SSH_KEY = credentials('78c238e3-0e4f-4ce2-8acd-cfdc3c4f68ca')
        S3_BUCKET = 'your-s3-bucket-name'
        DESTINATION_FOLDER = '/path/to/destination/folder'
        EC2_INSTANCE_PRIVATE_IP = 'EC2-Private-IP-Address'
    }

    stages {
        stage('Clean Destination Folder') {
            steps {
                script {
                    try {
                        sshagent(['78c238e3-0e4f-4ce2-8acd-cfdc3c4f68ca']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                'rm -rf ${DESTINATION_FOLDER}/*'
                            """
                        }
                        echo 'Destination folder cleaned successfully.'
                    } catch (Exception e) {
                        echo 'Error cleaning destination folder.'
                        error("Failed to clean the destination folder on EC2 instance. Please check the SSH connection and the folder path.")
                    }
                }
            }
        }
        /*
        stage('Copy Data from S3') {
            steps {
                script {
                    try {
                        sshagent(['78c238e3-0e4f-4ce2-8acd-cfdc3c4f68ca']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                'aws s3 cp s3://${S3_BUCKET}/ ${DESTINATION_FOLDER}/ --recursive'
                            """
                        }
                        echo 'Data copied from S3 to the destination folder successfully.'
                    } catch (Exception e) {
                        echo 'Error copying data from S3.'
                        error("Failed to copy data from S3 bucket to the EC2 instance. Please ensure that the S3 bucket exists and that the EC2 instance has appropriate permissions.")
                    }
                }
            }
        }

        stage('Restart Process with PM2') {
            steps {
                script {
                    try {
                        sshagent(['78c238e3-0e4f-4ce2-8acd-cfdc3c4f68ca']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                'pm2 restart all'
                            """
                        }
                        echo 'Process restarted successfully with PM2.'
                    } catch (Exception e) {
                        echo 'Error restarting process with PM2.'
                        error("Failed to restart the process with PM2 on the EC2 instance. Please check that PM2 is installed and correctly configured.")
                    }
                }
            }
        }*/


        
    } 

    post {
        failure {
            echo 'Pipeline failed. Please review the error messages and logs to identify the issue.'
        }
        success {
            echo 'Pipeline completed successfully.'
        }
    }
}

