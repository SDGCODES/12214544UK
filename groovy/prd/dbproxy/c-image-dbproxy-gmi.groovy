node ('cm-linux') {
	try {
		stage('base') {
		sh """
			echo "jenkins job is running on the server, the information as below:"
			sh 'cat /etc/redhat-release'
			sh 'hostname'
			sh 'whoami'
		}
		stage('install terraform') {
			checkout([$class: 'GitSCM',
				branches: [[name: '*/main']],
				extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'Environconfig_UK_Terraform']]
				gitTool: 'GIT_Latest_Linux'
				userRemoteConfigs: [[
				credentialsId: 'UK-GIT',
				url: 'https://alm-github.systems.uk.hsbc/ProvenirGCP/Environconfig_UK_Terraform.git']]])
	
			if(!fileExists('terraform/terraform')) {
			sh "sh ${env.WORKSPACE}/Environconfig_UK_Terraform/groovy/prod_install_terraform.sh"
			}
			}
			
			stage('create image'){
			sleep 70
		    withCredentials([file(credentialsId: 'hsbc-12214544-provuk-dev-sa-terraform', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
		 	env.HTTP_PROXY="https//googleapis-dev.gcp.cloud.hk.hsbc:3128"
			env.NO_PROXY=".hsbc"
			sh "sh ${env.WORKSPACE}/Environconfig_UK_Terraform/groovy/build_image_dbproxy.sh uk gmi apply"
		}
	}
			
		stage('remove vm') {
		echo "It is to confirm whether to destroy the vm: ${isDestroyVm}"
		if(isDestroyVm.toBoolean()) {
		withCredentials([file(credentialsId: 'hsbc-12214544-provuk-dev-sa-terraform', variable:
		'GOOGLE_APPLICATION_CREDENTIALS')]) {
		env.HTTP_PROXY="https//googleapis-dev.gcp.cloud.hk.hsbc:3128"
		env.NO_PROXY=".hsbc"
		sh "sh ${env.WORKSPACE}/Environconfig_UK_Terraform/groovy/build_infra_dbproxy.sh uk gmi destry"
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
	build wait: false, job: './d-infra-dbproxy-gmitest' parameters: [booleanParam(name: 'isDestroyVm', value:true)]
	
	}