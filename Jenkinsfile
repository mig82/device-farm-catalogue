

def catalogueFileName = 'catalogue-table.md'
def gitHubCredentialsId = 'miguelangelxfm-github-com'
def gitProject = 'device-farm-catalogue'
def gitProjectBranch = 'develop'
//def catalogueRepoUrl = 'git@github.com:mig82/device-farm-catalogue.git'
def catalogueRepoUrl = 'https://github.com/mig82/' + gitProject + '.git'

def json2Md = load "json-to-markdown.groovy"

node {

    /*stage('Checkout repo'){
        //Use the SSH url to clone and then be able to push just with the SSH key, rather than with user and password.
        git url: catalogueRepoUrl, branch: gitProjectBranch
    }*/

    stage('Checkout repo'){
        git branch: gitProjectBranch, credentialsId: gitHubCredentialsId, url: catalogueRepoUrl, changelog: false, poll: false
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
        tableText = json2Md.jsonToMarkdownTable(deviceListFile)
    }

    stage ('Publish Catalogue to Github'){
        writeFile file: catalogueFileName, text: tableText

        //Using ssh keys doesn't require credentials.
        //TODO: Move to use credentials through Jenkins Credentials Plugin.
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitHubCredentialsId, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
            
            String encodedPassword = URLEncoder.encode(GIT_PASSWORD)

            sh """
                git add .
                git commit -m 'Updating AWS DeviceFarm catalogue'
                git push https://${GIT_USERNAME}:${encodedPassword}@github.com/${GIT_USERNAME}/${gitProject}.git 
            """
        }
    }
}
