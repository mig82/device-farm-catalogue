properties([
    [
        $class: 'BuildDiscarderProperty',
        strategy: [
            $class: 'LogRotator',
            artifactDaysToKeepStr: '',
            artifactNumToKeepStr: env.MAX_ARTIFACTS_TO_KEEP?:'10',
            daysToKeepStr: '',
            numToKeepStr: env.MAX_JOBS_TO_KEEP?:'10'
        ]
    ],
    parameters([
        string(
            name: 'CATALOGUE_REPO_URL',
            defaultValue: params.CATALOGUE_REPO_URL?:'',
            description: 'The Git URL of the repository where you want to publish the device catalogue.'
        ),
        string(
            name: 'BRANCH',
            defaultValue: params.BRANCH?:'',
            description: 'The branch of the Git repo where you want to publish the device catalogue.'
        ),
        [
            $class: 'CredentialsParameterDefinition',
            name: 'GIT_CREDENTIALS',
            credentialType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl',
            defaultValue: params.GIT_CREDENTIALS?:'',
            description: '',
            required: true
        ],
        [
            $class: 'CredentialsParameterDefinition',
            name: 'AWS_CREDENTIALS',
            credentialType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl',
            defaultValue: params.AWS_CREDENTIALS?:'',
            description: 'The AWS key and secret you\'ll use to query the devices from DeviceFarm',
            required: true
        ]
    ])
])

def abort = false
if(currentBuild.number == 1 || params == null){
    currentBuild.result = 'SUCCESS'
    currentBuild.description = "'Configuration run'"
    echo("Is this the first build? The input parameters should be defined now. Please try again.")
    abort = true
}
if(abort)return;//This breaks the execution of the build.

node {

    //def awsHome = '/Library/Frameworks/Python.framework/Versions/3.5/bin/' //For Mac OS X
    //def awsHome = 'C:\\Program Files\\Amazon\\AWSCLI\\' //For Windows
    def awsHome = '' // If AWS CLI is already in the path.
    
    def gitProtocol, gitDomain, orgName, gitProject
    def gitCredId =  params.GIT_CREDENTIALS //Credentials parameter
    def catalogueRepoUrl = params.CATALOGUE_REPO_URL //String parameter
    def gitBranch = params.BRANCH //'master' //String parameter
    def catalogueFileName = 'catalogue-table.md'


    stage('Parse git URL'){
        echo catalogueRepoUrl
        def gitParams = catalogueRepoUrl.split('/')
        
        //def gitProtocol = 'https:'
        gitProtocol = gitParams[0]
        echo "gitProtocol=[${gitProtocol}]"
        
        //gitParams[1] is empty string between // after https:

        //def gitDomain = 'engie-src.ci.konycloud.com' //or the equivalent of 'github.com'
        gitDomain = gitParams[2]
        //def orgName = 'Corporate-Reusables'
        orgName = gitParams[3]
        //def gitProject = 'device-farm-catalogue'
        gitProject = gitParams[4].split('\\.')[0]
    }

    stage('checking for AWS'){
        sh "${awsHome}aws --version"
    }
    stage('checking for Git'){
        sh "git --version"
    }
    
    stage('Clean up'){
        sh """
            pwd
            ls -la 
            rm -rf ${gitProject}
            rm -f list-devices.json
            rm -f catalogue-table.md
        """
    }

    stage('Checkout repo'){

        dir(gitProject){
            git(
                credentialsId: gitCredId,
                poll: false,
                url: catalogueRepoUrl,
                branch: gitBranch
            )
        }
    }
    
    stage('Get Device Catalogue'){

        /*withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: AWS_CREDENTIALS, usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh """
                export AWS_ACCESS_KEY_ID='${AWS_ACCESS_KEY_ID}'
                export AWS_SECRET_ACCESS_KEY='${AWS_SECRET_ACCESS_KEY}'
            """
        }*/

        sh ("""
            cd ${gitProject}
            pwd
            ${awsHome}aws --region 'us-west-2' devicefarm list-devices > list-devices.json
            echo 'done listing devices'
            ls -la
        """)
    }

    def tableText = ""

    stage('Parse Device Catalogue'){

        def deviceListFile = readFile("./" + gitProject + "/list-devices.json")
        echo "Done reading list-devices.json file"

        sh """
            pwd
            ls -la
        """

        def json2Md = load("./" + gitProject + "/json-to-markdown.groovy")
        echo "Done loading json-to-markdown.groovy script"
        tableText = json2Md.jsonToMarkdownTable(deviceListFile)
        echo "Done parsing devices into markdown table format"
    }

    stage ('Publish Catalogue to Github'){
        
        writeFile file: (gitProject + "/" + catalogueFileName), text: tableText

        //Using ssh keys doesn't require credentials.
        //TODO: Move to use credentials through Jenkins Credentials Plugin.
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: GIT_CREDENTIALS, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
            
            //If password contains '@' character it must be encoded to avoid being mistaken by the '@' that separates user:password@url expression.
            String password = GIT_PASSWORD.contains("@") ? URLEncoder.encode(GIT_PASSWORD) : GIT_PASSWORD

            //Add changes and commit.

            def dirty = sh (
                script: """
                    pwd
                    ls -la
                    cd ${gitProject}
                    pwd
                    ls -la
                    git diff --exit-code
                """,
                returnStatus: true
            ) != 0

            echo "dirty=${dirty}"

            if(dirty){
                sh ("""
                    cd ${gitProject}
                    git status
                    git add .
                    git commit -m 'Updating AWS DeviceFarm catalogue'
                    git push ${gitProtocol}//${GIT_USERNAME}:${password}@${gitDomain}/${orgName}/${gitProject}.git
                """)

                sh("Done pushing changes.")
            }
            else {
                echo "No changes to commit."
            }  
        }
    }
}
