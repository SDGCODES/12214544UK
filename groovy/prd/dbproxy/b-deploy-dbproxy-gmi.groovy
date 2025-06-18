node ('cm-linux') {
	try {
		stage('base') {
		sh """
			echo "jenkins job is running on the server, the information as below:"
			sh 'cat /etc/redhat-release'
			sh 'hostname'
			sh 'whoami'
		}
		stage('connect VM and deploy') {
			sleep 60
			withCredentials([usernamePassword(credentialsId: 'UK-GIT', passwordVariable: 'GIT_PWD', usernameVariable: 'GIT_USR')]) {
				
				def GIT_URL="https://${GIT_USR}:${GIT_PWD}@alm-github.systems.uk.hsbc/ProvenirGCP/EnvConfig_UK_Provenir.git
				def GIT_SAVE_PATH="/tmp/workspace/EnvConfig_UK_Provenir"
				
			withCredentials([sshUserPrivateKey(credentialsId: 'ssh_ppk_uk_dev', keyFileVariable: 'SSH_PK', usernameVariable: 'SSH_USRNAME')])  {

				sh """
				
					set -x
					
					
					ssh -o StrictHostKeyChecking=no -tt -l ${SSH_USRNAME} -i ${SSH_PK}
					gce-provenir-uk-dbproxy-gmi.hsbc-12214544-provuk-dev.gcp.cloud.uk.hsbc << ENDSSH
					whoami
					
					sudo bash
					echo "2. install git application"
					yum install -y git
					
					echo "3. prepare temp workspace"
					rm -rf /tmp/workspace
					mkdir -p /tmp/workspace
					
					echo "5. clone environment configuration from git"
					rm -rf ${GIT_SAVE_PATH}
					echo "will clone ${GIT_URL} to ${GIT_SAVE_PATH}"
					git clone ${GIT_URL} ${GIT_SAVE_PATH}
					
					
					cd ${GIT_SAVE_PATH}
					
					echo "6. execute shell script to deploy dbproxy"
					sh deploy_pre.sh
					sh deploy-dbproxy.sh uk prd
					sh healthcheck_dbproxy.sh prd
					shutdown 1
					echo "log out root user and the remote server"
					# logout root user
					exit
					# logout remote server
					exit
					ENDSSH
				"""
				}
			}
		}
						
			currentBuild.result='SUCCESS'
			notifySuccess();
		  } catch(Exception err) {
			echo " [ERROR EXCEPTION CATCH]: ${err}"
			currentBuild.result='FAILURE'
			
			notifyFailed();
		}
	}
	
def notifyFailed() {
	print("This job built failed, stop next job and send mails to notify people")
	
	emailext (l
	subject: "Failed Pipeline * ${currentBuild.fullDisplayName}",
	body: "${currentBuild.projectName} got some error, please check log: ${env.BUILD_URL}."
	to: 'soumi.dasgupta@noexternalmail.hsbc.com;soumi.dasgupta@notreceivingmail.hsbc.com'
	}
	}
	

def buildNextJob() {
	print("This job built Success, will trigger next job")
	build wait: false, job: './c-image-dbproxy-gmi' parameters: [booleanParam(name: 'isDestroyVm', value:true)]
	
	}