
node {

    def catalogueFileName = 'catalogue-table.md'
    def gitHubCredentialsId = 'miguelangelxfm-github-com'
    def gitProject = 'device-farm-catalogue'
    def gitProjectBranch = 'develop'
    //def catalogueRepoUrl = 'git@github.com:mig82/device-farm-catalogue.git'
    def catalogueRepoUrl = 'https://github.com/mig82/' + gitProject + '.git'

    /*stage('Checkout repo'){
        //Use the SSH url to clone and then be able to push just with the SSH key, rather than with user and password.
        git url: catalogueRepoUrl, branch: gitProjectBranch
    }*/

    stage('Checkout repo'){
        git branch: gitProjectBranch, credentialsId: gitHubCredentialsId, url: catalogueRepoUrl, changelog: false, poll: false
        sh """
            git config user.email 'miguelangelxfm@gmail.com'
            git config user.name 'mig82'
            git config push.default simple
        """
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
        echo "Done reading list-devices.json file"
        def json2Md = load "json-to-markdown.groovy"
        echo "Done loading json-to-markdown.groovy script"
        tableText = json2Md.jsonToMarkdownTable(deviceListFile)
    }

    stage ('Publish Catalogue to Github'){
        writeFile file: catalogueFileName, text: tableText

        //Using ssh keys doesn't require credentials.
        //TODO: Move to use credentials through Jenkins Credentials Plugin.
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitHubCredentialsId, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
            
            String encodedUser = URLEncoder.encode(GIT_USERNAME)
            String encodedPassword = URLEncoder.encode(GIT_PASSWORD)

            sh "pwd"
            echo "${env.J_CREDS_IDS}"
            echo "${env.J_EMAIL}"
            echo "${env.J_USERNAME}"
            echo "${env.J_GIT_CONFIG}"
            echo "${env.BRANCH_NAME}"

            sh """
                git status
                git add .
                git commit -m 'Updating AWS DeviceFarm catalogue'
                git push https://${GIT_USERNAME}:${encodedPassword}@github.com/${GIT_USERNAME}/${gitProject}.git 
            """
        }
    }
}
