
node {

    //def awsHome = '/Library/Frameworks/Python.framework/Versions/3.5/bin/' //For Mac OS X
    //def awsHome = 'C:\\Program Files\\Amazon\\AWSCLI\\' //For Windows
    def awsHome = '' // If AWS CLI is already in the path.
    
    def gitProtocol, gitDomain, orgName, gitProject
    def gitCredId = GIT_CREDENTIALS //Credentials parameter
    def catalogueRepoUrl = CATALOGUE_REPO_URL //String parameter
    def gitBranch = BRANCH //'master' //String parameter
    def catalogueFileName = 'catalogue-table.md'

    stage('Validate input parameters'){
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

        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredId, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
        
            //Can't use git branch step because it attempts to use git.exe from master instead of git installed on linux slave.
            //git branch: gitProjectBranch, credentialsId: gitHubCredentialsId, url: catalogueRepoUrl, changelog: false, poll: false
            sh """
                git clone ${gitProtocol}//${GIT_USERNAME}:${GIT_PASSWORD}@${gitDomain}/${orgName}/${gitProject}.git
                echo 'Done cloning'
                cd ${gitProject}
                git checkout ${gitBranch}
                git config user.name ${GIT_USERNAME}
                git config push.default simple
                git config -l
                ls -la
            """

            //def catalogueRepoUrl = 'git@engie-src.ci.konycloud.com:Corporate-Reusables/device-farm-catalogue.git'
            //TODO: Use SSH or HTTPS based on input parameters.
            /*stage('Checkout repo'){
                //Use the SSH url to clone and then be able to push just with the SSH key, rather than with user and password.
                git url: (gitProtocol + catalogueRepoUrl), branch: gitProjectBranch
            }*/
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
