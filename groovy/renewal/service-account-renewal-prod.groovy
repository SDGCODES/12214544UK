import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import java.nio.file.*;

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
		withCredentials(file(credentialsId: "${idOfCredential}", variable: 'KEY_FILE')]) {
			sh """
				gcloud config set proxy/address googleapis-prod.gcp.cloud.hk.hsbc
				gcloud config set proxy/port 3128
				gcloud config set proxy/type http_no_tunnel
				gcloud auth activate-service-account ${serviceAccount} --key-file=${KEY_FILE}
				gcloud config set project ${project}
				
		
		stage('create new key-file') {
			sh """
				echo "list key before recreate key"
				gcloud iam service-account keys list --iam-account ${serviceAccount} --managed-by=user --sort-by=CREATED_AT
				gcloud iam service-account keys create ${keyFileName} --iam-account ${serviceAccount} --quiet
				gcloud iam service-account keys list --iam-account ${serviceAccount} --managed-by=user --sort-by=CREATED_AT
				cat ${keyFileName}
			   """
			  }
			  
		stage('delete old key-file') {
			KEY=sh(returnStdoubt: true, script: "gcloud iam service-account keys list --iam-account ${serviceAccount} --managed-by=user --limit=1 --format 'table[no-heading] {KEY_ID, CREATED_AT:sort=1)' | cut -d' ' -f1").trim()
			sh "gcloud iam service-accounts keys delete ${KEY} --iam-account ${serviceAccount} --quiet"
			
			}
			
		stage('update credentials') {
			jsonContent = readFile "${keyFileName}"
			def secretBytes = secretBytes.fromBytes(jsonContent.getBytes())
			
			def fcs = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
				org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl.class,
				getFolder(forderName)
			}
			
			def gcpSAToken
			for (org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl fileCred : fcs) {
			
			IF(fileCred.id == idOfCredential) {
			 gcpSAToken = fileCred
			 }
			}
			
			def updatedCredentials = new FileCredentialsImpl(gcpSAToken.scope, gcpSAToken.id, gcpSAToken.description, gcpSAToken.fileName, secretBytes)
			updateFolderCredentials(forderName, gcpSAToken, updatedCredentials)
			}
			
			currentBuild.result='SUCCESS'
			notifySuccess();
		  } catch(Exception err) {
			echo " [ERROR EXCEPTION CATCH]: ${err}"
			currentBuild.result='FAILURE'
			
			notifyFailed();
		}
	}
	
@NonCPS
getFolder(folderName) {
	def cloudbeesFolder
	def allJenkinsItems = Jenkins.getInstance().getItems();
	for (currentJenkinsItem in allJenkinsItems)
	{
	if(currentJenkinsItem != null && currentJenkinsItem instanceof Folder)
	{
	if(currentJenkinsItem.toString().contains(folderName))
	cloudbeesciFolder = (Folder)currentJenkinsItem
	}
	
	}
	return cloudbeesciFolder
		
@NonCPS
updateFolderCredentials (folderName, oldCred, updatedCred) {

	def credentials_store = Jenkins.instance.getExtensionList(
	'com.cloudbees.hudson.plugins.folder.properties.FileCredentialsProvider'
	) [0].getStore(getFolder(folderName))
	
result = credentials_store.updatedCredentials(
com.cloudbees.plugins.credentials.domains.Domain.global(),
oldCred,
updatedCred
)

println "Update Result= $result"
}

def notifySuccess() {
	print("This job built successful")
	
	def mailTemplate = "Hi,\n\n"
	mailTemplate += "New key has been provisioned for service Account: ${env.serviceAccount} .\n\n"
	mailTemplate += "Below the key for your reference:\n\n${jsonContent} .\n\n"
	
	emailext (
	subject: "SA key Renew for ${env.project}",
	body: "mailTemplate"
	to: 'soumi.dasgupta@noexternalmail.hsbc.com;soumi.dasgupta@notreceivingmail.hsbc.com'
	}
	}
def notifyFailed() {
	print("This job built failed, mails to notify")
	
	emailext (l
	subject: "Failed Pipeline * ${currentBuild.fullDisplayName}",
	body: "${currentBuild.projectName} got some error, please check log: ${env.BUILD_URL}."
	to: 'soumi.dasgupta@noexternalmail.hsbc.com;soumi.dasgupta@notreceivingmail.hsbc.com'
	}
	}