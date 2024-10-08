pipeline {
    agent any

    environment {
        SSH_KEY = credentials('SSH_KEY')
        S3_BUCKET = credentials('S3_BUCKET')
        DESTINATION_FOLDER = credentials('DESTINATION_FOLDER')
        EC2_INSTANCE_PRIVATE_IP = credentials('EC2_INSTANCE_PRIVATE_IP')
    }

    stages {

        stage('Send Approval Request') {
            steps {
                script {
                    emailext (
                        subject: 'Approval Required: Deploy to Production',
                        body: '''<p>Deployment is ready for production. Please approve or reject the deployment:</p>
                                 <p><a href="http://your-approval-system/approve">Approve</a></p>
                                 <p><a href="http://your-approval-system/abort">Abort</a></p>''',
                        to: 'bsoomee.2011@gmail.com'
                    )
                    echo 'Approval request sent via email.'
                }
            }
        }

        stage('Manual Approval') {
            steps {
                script {
                    input message: 'Do you want to proceed with the deployment?', ok: 'Approve'
                    echo 'Approval received. Proceeding with the deployment.'
                }
            }
        }

        stage('Clean Destination Folder') {
            steps {
                script {
                    try {
                        sshagent(['SSH_KEY']) {
                            sh '''
                                if [[ "$DESTINATION_FOLDER" != "/" && "$DESTINATION_FOLDER" != "" ]]; then
                                    ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                    "echo 'Cleaning destination folder: ${DESTINATION_FOLDER}' && \
                                    sudo rm -rf ${DESTINATION_FOLDER}/*"
                                else
                                    echo "Error: DESTINATION_FOLDER is set to root or empty, aborting cleanup."
                                    exit 1
                                fi
                            '''
                        }
                        echo 'Destination folder cleaned successfully.'
                    }
                    catch (Exception e) {
                        echo 'Error cleaning destination folder.'
                        error("Failed to clean the destination folder on EC2 instance. Please check the SSH connection or the folder path.")
                    }
                }
            }
        }
        
        stage('Copy Data from S3') {
            steps {
                script {
                    try {
                        sshagent(['SSH_KEY']) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                "sudo aws s3 cp s3://${S3_BUCKET}/ ${DESTINATION_FOLDER}/ --recursive"
                            '''
                        }
                        echo 'Data copied from S3 to the destination folder successfully.'
                    } catch (Exception e) {
                        echo 'Error copying data from S3.'
                        error("Failed to copy data from S3 bucket to the EC2 instance. Please ensure that the S3 bucket exists and that the EC2 instance has appropriate permissions.")
                    }
                }
            }
        }


        /* 
        stage('Restart Process with PM2') {
            steps {
                script {
                    try {
                        sshagent(['SSH_KEY']) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE_PRIVATE_IP} \
                                'sudo pm2 restart all'
                            '''
                        }
                        echo 'Process restarted successfully with PM2.'
                    } catch (Exception e) {
                        echo 'Error restarting process with PM2.'
                        error("Failed to restart the process with PM2 on the EC2 instance. Please check that PM2 is installed and correctly configured.")
                    }
                }
            }
        } */


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
