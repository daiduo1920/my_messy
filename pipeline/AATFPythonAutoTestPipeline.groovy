package com.bba.atc.jenkins.steps.pytest

import com.bba.atc.jenkins.steps.common.GitCheckout


/**
 * @Author Zhang yunlong
 * @Date 4/11/2023
 * @Description
 */
class AATFPythonAutoTestPipeline {
    def pythonATest

    AATFPythonAutoTestPipeline(pythonATest) {
        this.pythonATest = pythonATest
    }
    
   
    
    def runAATFPythonTest(env){
    	pythonATest.dir(env.run_path){
			pythonATest.echo "[Run AATF Python Test][INFO]: Create report.json file========"
			try{
				if (!Boolean.valueOf(env.useProxy.toString())) {
					pythonATest.echo "[Run AATF Python Test][INFO]: Test Without Proxy"
					pythonATest.bat 'python -m pytest bdd/test_runner.py -v --cucumber-json=Reports/cucumber.json'
				}
				pythonATest.bat 'python addScreenToJson.py'
			}catch(all){
				pythonATest.echo "[Run AATF Python Test][INFO]: Python addScreenToJson"
				pythonATest.bat 'python addScreenToJson.py'
			}
		}
    }// runAATFPythonTest
   
    
    def sendTestEmail(env){
		def eDate = new Date()
		def tookTime = eDate.getTime()-Long.parseLong(env.startDateTime)
		def endDate = eDate.format('dd/MM/yyyy HH:mm:ss')
		def totalNum = 0
		def failedNum = 0
		def passedNum = 0
		pythonATest.echo "[Send Test Emails When Jenkins Build Success][INFO]: StartDate(${env.startDate}),EndDate(${endDate}),tookTime(${tookTime}) " 
                
        def sendTestFailMail = {
			emailTemplate,isTestCaseFail ->
				if (Boolean.valueOf(env.isSendMail.toString()) && (env.mailTo.trim().length() > 0)) {
					if(isTestCaseFail) {
						pythonATest.emailext attachmentsPattern: 'error_report.html', 
							body:emailTemplate, 
			                subject: env.mailSubject ,
			                to: env.mailTo ,
			                from: 'ATC.AutoNotification@bmw-brilliance.cn'
					}else{
						pythonATest.emailext body:emailTemplate, 
			                subject: env.mailSubject ,
			                to: env.mailTo ,
			                from: 'ATC.AutoNotification@bmw-brilliance.cn'
					}
	            }
        }
				
		def testFail = {
            element ->
                for (before in element.before) {
                    if (before.result.status == "failed") { return before }
                }
                for (step in element.steps) {
                    if (step.result.status == "failed") { return step }
                }
                for (after in element.after) {
                    if (after.result.status == "failed") { return after }
                }
                return null
        }
		
		def isTestCaseFail = false
		def cucumber_datas = null
		pythonATest.dir(env.run_path){
			cucumber_datas = pythonATest.readJSON file: 'Reports/cucumber.json'
			
		}
		def cucumber_result = ''
		def cucumber_error_result = ''
				
		for (data in cucumber_datas) {
			for(element in data.elements){
				totalNum++
				def failElement = testFail(element)
				if(failElement!=null && failElement.result.status == "failed"){
					isTestCaseFail = true
					failedNum++
					def error_temp_result = '''
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						<tr bgcolor="#DCDCDC">
							<td width="160"><b><font color="#8E6B23">[Scenario] :</font></b><td>
							<td>''' + element.name + '''</td>
							<td width="15%" align="right"><b><font color="Red">FAILED</font></b></td>
						</tr>
						</table>
					'''
					cucumber_result = cucumber_result + error_temp_result
					cucumber_error_result = cucumber_error_result + error_temp_result
					
					def error_msg = failElement.result.error_message.toString()
					def sub_error_msg = error_msg
					if(error_msg.length() > 256){
						sub_error_msg = error_msg.substring(0,256)
					}
					error_temp_result = '''
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						<tr>
							<td width="10"><td>
							<td width="150"><font color="Red"><li>Failed Step: </li></font></td>
							<td bgcolor="#F5F5DC">'''+failElement.name+'''</td>
						</tr>
						<tr>
							<td width="10"><td>
							<td width="150"><font color="Red"><li>Error Message: </li></font></td>
							<td></td>
						</tr>
						<tr>
							<td width="10"><td>
							<td width="150"></td>
							<td bgcolor="#F5F5DC">'''+sub_error_msg+'''... ...</td>
						</tr>
						</table>
					'''
					cucumber_result = cucumber_result + error_temp_result
					error_temp_result = '''
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						<tr>
							<td width="10"><td>
							<td width="150"><font color="Red"><li>Failed Step: </li></font></td>
							<td bgcolor="#F5F5DC">'''+failElement.name+'''</td>
						</tr>
						<tr>
							<td width="10"><td>
							<td width="150"><font color="Red"><li>Error Message: </li></font></td>
							<td></td>
						</tr>
						<tr>
							<td width="10"><td>
							<td width="150"></td>
							<td bgcolor="#F5F5DC">''' + error_msg + '''</td>
						</tr>
						</table>
					'''
					cucumber_error_result = cucumber_error_result + error_temp_result
				}else{
					passedNum++
					cucumber_result = cucumber_result + '''
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						<tr bgcolor="#DCDCDC">
							<td width="160"><b><font color="#8E6B23">[Scenario] :</font></b><td>
							<td>''' + element.name + '''</td>
							<td width="15%" align="right"><b><font color="Green">PASSED</font></b></td>
						</tr>
						</table>
					'''
				}
			}
		}
		
		cucumber_result = cucumber_result.replace('${', '__{')
		
		def emailTemplate = '''<body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4" offset="0">
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
							<tr>
								<td>
									<div>
										<font color="Green"><b>[Automation Test Run Infomation] : </b></font>
										<ul>
											<li>Jira Issue: <a href="'''+env.JIRA_URL+'''/browse/${issueKey}">'''+env.JIRA_URL+'''/browse/${issueKey}</a></li>
											<li>Test Run StartTime: '''+env.startDate+'''</li>
											<li>Test Run EndTime: '''+endDate+'''</li>
											<li>Test Run UsedTime: '''+tookTime+''' ms</li>
											<li>Test Result Status: <b><font color="Green">[PASSED] : '''+passedNum+'''</font> , <font color="Red">[FAILED] : '''+failedNum+'''</font> , <font color="Blue">[Total] : '''+totalNum+'''</font></b></li>
										</ul>
										<hr size="2" width="100%" align="center" />
									</div>
								</td>
							</tr>
						</table>
						<div>
							<font color="Green"><b>[Automation Test Report] : </b></font>
						<div>
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						'''+cucumber_result+'''
						</table>
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
							<tr>
								<td>
									<div>
										<hr size="2" width="100%" align="center" />
										<font color="Green"><b>[Jenkins Build Summary] : </b>
										<ul>
											<li>Jenkins Full Project Name: ${PROJECT_NAME}</li>
											<li>Jenkins Job Build Number: # ${BUILD_NUMBER}</li>
											<li>Jenkins Job Build Cause: ${CAUSE}</li>
											<li>Jenkins Job Build Status: ${BUILD_STATUS}</li>
											<li>Build Log: <a href="${BUILD_URL}console">${BUILD_URL}console</a></li>
										</ul>
										</font>
										<hr size="2" width="100%" align="center" />
									</div>
								</td>
							</tr>
						</table>
                </body>'''
        def error_report = '''
        				<body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4" offset="0">
						<div>
							<font color="Red"><b>[Failure Reports] : </b></font>
						</div>
						'''+cucumber_error_result+'''
						<table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
						<hr size="2" width="100%" align="center" />
						</table>
						</body>
				'''
        

		if(Boolean.valueOf(env.onlySendMailOnFailure.toString())){
			if(isTestCaseFail){
				pythonATest.echo "Build Result : SUCCESS , SendMailOnFailure , Test Case Fail: " + isTestCaseFail
				pythonATest.writeFile file: 'error_report.html', text: error_report
				sendTestFailMail(emailTemplate,isTestCaseFail)
			}
		}else{
			pythonATest.echo "Build Result : SUCCESS , SendMailOnSuccess , Test Case Fail: " + isTestCaseFail
			if(isTestCaseFail){
				pythonATest.writeFile file: 'error_report.html', text: error_report
				sendTestFailMail(emailTemplate,isTestCaseFail)
			}else{
				sendTestFailMail(emailTemplate,false)
			}
		}
    }// sendTestEmail
    
    def runAfterAATFPytestTest(env){
    	pythonATest.dir(env.run_path){
    		pythonATest.archiveArtifacts "bdd\\bba_feature\\*.feature"
    		pythonATest.archiveArtifacts "Reports\\*.json"
    	}
    }// runAfterTest
    
    def windowsCurl_UpdateIssueStatus(env){
    	pythonATest.bat """
              curl -s -k GET "https://10.188.219.130:8443/jenkinsbe/buildByToken/buildWithParameters?job=Xray-Auto-Testing/Update_Xray_Issue_status&token=UpdateXrayIssueStatus_201808081502&issueKey="""+env.issueKey+"""&buildResult=${env.currentBuildResult}"
          """
    }// curlUpdateIssueStatus
    
    def buildJob_UpdateIssueStatus(env){
        pythonATest.build job: 'Xray-Auto-Testing/Update_Xray_Issue_status', parameters: [pythonATest.string(name: 'issueKey', value: "$env.issueKey"), pythonATest.string(name: 'buildResult', value: "${env.currentBuildResult}")],wait:false
    }// buildUpdateIssueStatus
    
    def sendJenkinsBuildErrorEmail(env){
    	pythonATest.echo "[Post Always Send Jenkins Build Failure Emails][ERROR]: Build Result : Failure "
		env.mailTo = env.mailTo + ';yunlong.zhang.ic@partner.bmw-brilliance.cn'
		
        def emailTemplate = '''<body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4" offset="0">
			                    <table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
			                        <tr>
			                            <td>
			                                <div>
												<font color="Green"><b>[Automation Test Run Infomation] : </b>
												<ul>
													<li>Jira Issue: <a href="'''+env.JIRA_URL+'''/browse/${issueKey}">'''+env.JIRA_URL+'''/browse/${issueKey}</a></li>
													<li>Jenkins Full Project Name: ${PROJECT_NAME}</li>
													<li>Jenkins Job Build Number: # ${BUILD_NUMBER}</li>
													<li>Jenkins Job Build Cause: ${CAUSE}</li>
													<li>Jenkins Job Build Status: ${BUILD_STATUS}</li>
													<li>Build Log: <a href="${BUILD_URL}console">${BUILD_URL}console</a></li>
												</ul>
												</font>
												<hr size="2" width="100%" align="center" />
											</div>
			                            </td>
			                        </tr>
			                    </table>
			               </body>'''
			               
        pythonATest.emailext body: emailTemplate,
                subject: env.mailSubject,
                to: env.mailTo,
                from: 'ATC.AutoNotification@bmw-brilliance.cn'
        pythonATest.jiraAddComment(idOrKey: env.issueKey, comment: 'Jenkins Pipeline Build Failure.')
    }// sendJenkinsBuildErrorEmail
}