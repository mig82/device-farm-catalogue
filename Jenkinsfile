import groovy.json.JsonSlurper

@NonCPS
def jsonToMarkdownTable(txt){
    
    //Parse the text into JSON, get the devices property and sort by name.
    def devices = new groovy.json.JsonSlurper().parseText(txt).devices.toSorted { a, b -> a.name <=> b.name }
    
    //This is for adding new lines.
    def newline = "\n"
    
    //This is the header of the Markdown table.
    def header =
        "| Name | Form Factor | Platform | ARN | Model | OS Version | Manufacturer |" +
        newline +
        "| --- | --- | --- | --- | --- | --- | --- |"
    
    //This is to build the table's body by concatenating a line for each device.
    def body = ""
    
    for (int k=0; k<devices.size(); k++){
        def d = devices[k];
        def line = "| ${d.name} | ${d.formFactor} | ${d.platform} | ${d.arn} | ${d.model} | ${d.os} | ${d.manufacturer} |"
        body += line + newline 
    }
    
    return header + newline + body
}

node {
    
    stage 'Get Device Catalogue'
    def awsHome = '/Library/Frameworks/Python.framework/Versions/3.5/bin/'
    sh """
        export AWS_ACCESS_KEY_ID='AKIAISRF63CQSS7GV2UQ'
        export AWS_SECRET_ACCESS_KEY='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'
        export AWS_DEFAULT_REGION='us-west-2'
        ${awsHome}aws devicefarm list-devices > list-devices.json
    """ 
    stage 'Parse Device Catalogue'
    def deviceListFile = readFile('list-devices.json')
    echo "Done reading file"
    def tableText = jsonToMarkdownTable(deviceListFile)
    
    stage 'Publish Catalogue to Github'
    writeFile file: 'catalogue-table.md', text: tableText

    stage('Push changes to Github repo'){
        env.BRANCH_NAME = "master"// BRANCH_NAME is predefined in multibranch pipeline job
        env.J_GIT_CONFIG = "true"
        env.J_USERNAME = "Mig82"
        env.J_EMAIL = "miguelangelxfm@gmail.com"
        env.J_CREDS_IDS = 'myGithubCredentials' // Use credentials id from Jenkins
        def gitLib = load "git_push_ssh.groovy"
        gitLib.pushSSH(commitMsg: "Jenkins build #${env.BUILD_NUMBER}", tagName: "build-${env.BUILD_NUMBER}", files: "README.md");

    }
}