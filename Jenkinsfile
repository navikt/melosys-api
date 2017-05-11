timestamps {
	def application = "melosys"

    def username, password
    
    def committer, committerEmail, changelog, pom, isSnapshot, nextVersion
    
    def s_build = false
    def s_deploy = false
    def version = ''
    def environment = ''
    
    try {
    	
        s_build = Boolean.valueOf(BUILD)
        s_deploy = Boolean.valueOf(DEPLOY)
        fasitCredentialId = env.FASIT_CRED
        if (env.DEPLOY_VERSION != null) {
            version = env.DEPLOY_VERSION
        }
        if (env.ENVIRONMENT != null) {
            environment = env.ENVIRONMENT
        }

    } catch (MissingPropertyException e) {
        deploy = false
        throw e
    }
    
    
    node {

        try {
            env.LANG = "nb_NO.UTF-8"
            def mvnHome = tool "maven-3.5.0"
            env.PATH = "${mvnHome}/bin:${env.PATH}"            
            def artifactId = readFile('pom.xml') =~ '<artifactId>(.+)</artifactId>'
            artifactId = artifactId[0][1]

            stage("Init") {
                //printStage("Init")
                
                checkout scm
                
                pom = readMavenPom file: 'pom.xml'
                version = pom.version.tokenize("-")[0]
                isSnapshot = pom.version.contains("-SNAPSHOT")
                committer = sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
                committerEmail = sh(script: 'git log -1 --pretty=format:"%ae"', returnStdout: true).trim()
                changelog = sh(script: 'git log `git describe --tags --abbrev=0`..HEAD --oneline', returnStdout: true)
            }          

            if (s_build) {
                stage("Build") {
                    printStage("Build")
                    configFileProvider(
                        [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -B -V -U -e -s $MAVEN_SETTINGS clean deploy'
                    }
                }            

                info("Build ${artifactId}:${version}")
            }            
            
            if (s_deploy) {

                stage("Deploy") {
                    printStage("Deploy")                  
                    
                    callback = "${env.BUILD_URL}input/Deploy/"
 
					def deploy = deployApp(application, version, environment, callback, committer).key  
                    
                    try {
                        timeout(time: 15, unit: 'MINUTES') {
                            input id: 'deploy', message: "Check status here:  https://jira.adeo.no/browse/${deploy}"
                        }
                    } catch (Exception e) {
                        throw new Exception("Deploy feilet :( \n Se https://jira.adeo.no/browse/" + deploy + " for detaljer", e)

                    }                    

                    info("Deploy ${artifactId}:${version} to ${environment}")

                }
            }            
            
        } catch(error) {
            if (s_deploy) {
                info(environment)
            }                

            throw error
        }       
    }
}

Object deployApp(app, version, environment, callback, reporter) {
    def envMap = [
            'ussi1': '22579',
    ]
    parsedEnvironment = envMap[environment]

    println("Init deploy with the following input")
    println("Application: \t ${app}")
    println("Version: \t ${version}")
    println("Environment: \t ${environment} (translated to: ${parsedEnvironment})")
    println("On behalf of: \t ${reporter}")
    println("Will callback on ${callback}")

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.FASIT_CRED, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
    	log(env.USERNAME)
    
        def postBody = [
                fields: [
                        project          : [key: 'DEPLOY'],
                        issuetype        : [id: '10902'],
                        customfield_14811: [id: parsedEnvironment, value: parsedEnvironment],
                        customfield_14812: "${app}:${version}",
                        customfield_17410: callback,
                        summary          : "Automatisk deploy på vegne av ${reporter}"
                ]
        ]

        def postBodyString = groovy.json.JsonOutput.toJson(postBody)
        def base64encoded = "${env.USERNAME}:${env.PASSWORD}".bytes.encodeBase64().toString()


        def response = httpRequest url: 'https://jira.adeo.no/rest/api/2/issue/', customHeaders: [[name: "Authorization", value: "Basic ${base64encoded}"]], consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: postBodyString
        def slurper = new groovy.json.JsonSlurper()
        return slurper.parseText(response.content);
    }
}


void info(msg) {
    ansiColor('xterm') {
        println "\033[45m\033[37m " + msg + " \033[0m"
    }
    currentBuild.description = msg
}

void log(msg) {
    ansiColor('xterm') {
        println "\033[42m " + msg + " \033[0m"
    }
}

void printStage(stage) {
    ansiColor('xterm') {
        println "\033[46m Entered stage " + stage + " \033[0m"
    }
}