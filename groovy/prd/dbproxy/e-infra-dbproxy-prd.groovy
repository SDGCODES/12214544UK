import java.time.*
import java.util.*;
import java.text.SimpledateFormat;

def executeIfMatchCondition;
node ('cm-linux') {

	def INFRA_FAIL_UK_PROD = "[Infra renewal] Fail for UK PROD DBPROXY";
	def INFRA_SUCCESS_UK_PROD = "[Infra renewal] Success for UK PROD DBPROXY";
	try {
	if (!suspendExecution.toBoolean()) {
		executeIfMatchCondition = conditionForExecite();
		if (manuallyExecute.toBoolean() || executeIfMatchCondition) {
		stage('base') {
		sh """
			echo "jenkins job is running on the server, the information as below:"
			sh 'cat /etc/redhat-release'
			sh 'hostname'
			sh 'whoami'
		}
		stage('git clone') {
			checkout([$class: 'GitSCM',
				branches: [[name: '*/main']],
				extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'Environconfig_UK_Terraform']]
				gitTool: 'GIT_Latest_Linux'
				userRemoteConfigs: [[
				credentialsId: 'UK-GIT',
				url: 'https://alm-github.systems.uk.hsbc/ProvenirGCP/Environconfig_UK_Terraform.git']]])
				}
		stage('install terraform') {
			if(!fileExists('terraform/terraform')) {
			sh "sh ${env.WORKSPACE}/Environconfig_UK_Terraform/groovy/prod_install_terraform.sh"
			}
			}
			
		stage('create vm'){
		withCredentials([file(credentialsId: 'hsbc-12214544-provuk-prod-sa-terraform', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
			env.HTTP_PROXY="https//googleapis-dev.gcp.cloud.hk.hsbc:3128"
			env.NO_PROXY=".hsbc"
			sh "sh ${env.WORKSPACE}/Environconfig_UK_Terraform/groovy/build_infra_dbproxy.sh uk gmi apply"
		}
	}
		
			currentBuild.result='SUCCESS'
			emailNotification(INFRA_SUCCESS_UK_PROD);
			}
			else
			{
			echo "Today isn't sunday and the week number is not odd, prov env doesn't need to be rebuilt."
			}
			}
			
		  } catch(Exception err) {
			echo " [ERROR EXCEPTION CATCH]: ${err}"
			currentBuild.result='FAILURE'
			
			emailNotification(INFRA_FAIL_UK_PROD);
		}
	}
	
def emailNotification(emailSubject) {
	
	emailext (
	subject: "${emailSubject}",
	body:"The log: ${env.BUILD_URL}."
	to: 'soumi.dasgupta@noexternalmail.hsbc.com;soumi.dasgupta@notreceivingmail.hsbc.com'
	}
	}
	

def conditionForExecite() {
	def formatPattern = "yyyy-MM-dd'T'HH:mm:ss";
	def tz = TimeZone.getTimeZone("Europe/London");
	def ts = new Date();
	def datwStr = ts.format(formatPattern, timezone=tz);
	print dateStr;
	
	def parsed = new SimpledateFormat(formatPattern).parse(dateStr)
	Calender calender = parsed.toCalender();
	
	def WeekOfYear = calender.get(Calender.WEEK_OF_YEAR);
	def dayOfWeek = calender.get(Calender.DAY_OF_WEEK);
	
	def isEvenWeek = WeekOfYear % 1 == 0;
	def isSunday = dayOfWeek == calender.SUNDAY;
	
	return isEvenWeek && isSunday;
	}