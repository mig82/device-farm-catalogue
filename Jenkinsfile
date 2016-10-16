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

def catalogueFileName = 'catalogue-table.md'

node {

    stage('Checkout repo'){
        git url: 'https://github.com/mig82/device-farm-catalogue.git', branch: "develop"
    }
    
    stage('Get Device Catalogue'){
        //Get the path to the local installation of aws.
        def awsHome = '/Library/Frameworks/Python.framework/Versions/3.5/bin/'
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'devicefarm-readonly', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh """
                export AWS_DEFAULT_REGION='us-west-2'
                ${awsHome}aws devicefarm list-devices > list-devices.json
            """
        }
    } 
    
    def tableText = ""

    stage('Parse Device Catalogue'){
        def deviceListFile = readFile('list-devices.json')
        echo "Done reading file"
        tableText = jsonToMarkdownTable(deviceListFile)
    }

    stage ('Publish Catalogue to Github'){
        writeFile file: catalogueFileName, text: tableText

        //def workspace = pwd()
        //sh "cp ${workspace}/${catalogueFileName} ${workspace}@script/${catalogueFileName}"
        //echo "env.BRANCH_NAME is '${env.BRANCH_NAME}'"
        
        //env.BRANCH_NAME = "develop"// BRANCH_NAME is predefined in multibranch pipeline job
        //env.J_GIT_CONFIG = "true"
        //env.J_USERNAME = "mig82"
        //env.J_EMAIL = "miguelangelxfm@gmail.com"
        //env.J_CREDS_IDS = 'myGithubCredentials' // Use credentials id from Jenkins
        
        //def gitLib = load "${workspace}@script/git_push_ssh.groovy"
        /*def gitLib = load "git_push_ssh.groovy"
        //sh "cd ${workspace}@script"
        gitLib.pushSSH(commitMsg: "Jenkins build #${env.BUILD_NUMBER}", tagName: "build-${env.BUILD_NUMBER}", files: catalogueFileName);*/

        //git remote set-url origin git@github.com:${env.J_USERNAME}/device-farm-catalogue.git
        /*sh """
            git remote add origin https://${env.J_USERNAME}:T3quil%401@github.com/${env.J_USERNAME}/device-farm-catalogue.git  
            git config user.name ${env.J_USERNAME}
            git config user.email ${env.J_EMAIL}
            git add .
            git commit -m 'Updating AWS DeviceFarm catalogue'
            git push --set-upstream origin develop 
        """*/
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'miguelangelxfm-github-com', passwordVariable: 'GITHUB_PASSWD', usernameVariable: 'GITHUB_USER']]) {
            sh """
                git add .
                git commit -m 'Updating AWS DeviceFarm catalogue'
                git push --set-upstream origin develop 
            """
        }
    }
}
