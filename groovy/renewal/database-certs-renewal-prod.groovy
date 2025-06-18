node ('cm-linux') {
	try {
		stage('base information') {
		sh """
			echo "jenkins job is running on the server, the information as below:"
			cat /etc/redhat-release
			hostname
			whoami
			gcloud -v
		"""
		
		}
		
		env.project="hsbc-12214544-provuk-prod"
		env.serviceAccount = "terraform@hsbc-12214544-provuk-prod.iam.gserviceaccount.com"
		def keyFileName= "hsbc-12214544-provuk-prod-sa-terraform.json"
		def forderName="hsbc-12214544-uk"
		
		echo "gcloud configuration"
		withCredentials(file(credentialsId: 'hsbc-12214544-provuk-prod-sa-terraform', variable: 'KEY_FILE')]) {
			
			    sh "gcloud config set proxy/address googleapis-prod.gcp.cloud.hk.hsbc"
				sh "gcloud config set proxy/port 3128"
				sh "gcloud config set proxy/type http_no_tunnel"
				sh "gcloud auth activate-service-account ${serviceAccount} --key-file=${KEY_FILE}"
				sh "gcloud config set project ${project}"
				sh "gcloud config list"
				
				}
				
		
		stage('renew database cert for PROD env') {
		
				sh "gcloud config list"
				sh "gcloud beta sql ssl server-ca-certs create --instance=postgresql-provuk-prod"
				sh "gcloud beta sql ssl server-ca-certs rotate --instance=postgresql-provuk-prod"
			  }
		
		
		currentBuild.result='SUCCESS'
		emailNotification(CERT_SUCCESS_UK_PROD)
		}
		catch(Exception err) {
		echo "[ERROR EXCEPTION CATCH]: ${err}"
		currentBuild.result='FAILURE'
		emailNotification(CERT_FAIL_UK_PROD)
	}
	
}
		
def emailNotification(emailSubject) {
	
	emailext (
	subject: "${emailSubject}",
	body:"The log: ${env.BUILD_URL}."
	to: 'soumi.dasgupta@noexternalmail.hsbc.com;soumi.dasgupta@notreceivingmail.hsbc.com'
	}
}