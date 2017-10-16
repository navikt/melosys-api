timestamps {
	def application = "melosys"

    def username, password
    
    def committer, committerEmail, changelog
    def pom, artifactId, version, isSnapshot, nextVersion

    def opt_deploy = false
    def opt_sonar = false
    def environment = ''

    if (env.ENVIRONMENT != null) {
    	environment = env.ENVIRONMENT
    }
    if (env.DEPLOY != null) {
    	opt_deploy = Boolean.valueOf(DEPLOY)
    }
    if (env.SONAR != null) {
	    opt_sonar = Boolean.valueOf(SONAR)
    }
    
    node {

        try {
            env.LANG = "nb_NO.UTF-8"
            def mvnHome = tool "maven-3.5.0"
            env.PATH = "${mvnHome}/bin:${env.PATH}"

            stage("Init") {
                printStage("Init")

                checkout scm

                pom = readMavenPom file: 'pom.xml'
                version = pom.version.tokenize("-")[0]
                isSnapshot = pom.version.contains("-SNAPSHOT")
            	artifactId = pom.artifactId
                committer = sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
                committerEmail = sh(script: 'git log -1 --pretty=format:"%ae"', returnStdout: true).trim()
                changelog = sh(script: 'git log `git describe --tags --abbrev=0`..HEAD --oneline', returnStdout: true)
            }          


            stage("Build") {
                printStage("Build")
                configFileProvider(
                    [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -B -V -U -e -s $MAVEN_SETTINGS clean deploy'
                }

                info("Build ${artifactId}:${version}")
            }            
            
            if (opt_deploy) {

                stage("Deploy") {
                    printStage("Deploy")                  
                    
                    callback = "${env.BUILD_URL}input/Deploy/"
 					
                    if (isSnapshot) {
                    	version = version + "-SNAPSHOT"
                    }
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

            if (opt_sonar) {

                stage('CoCo') {
                    printStage("Code coverage")
                    configFileProvider(
                            [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
                        sh "mvn -P runSonar -s $MAVEN_SETTINGS clean verify -Dmaven.root=${env.WORKSPACE} "
                    }
                }

                stage("Sonar scan") {
                    printStage("Sonar scan")

                    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: env.SERVICE_USER,
                                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        username = env.USERNAME
                        password = env.PASSWORD
                    }

                    configFileProvider(
                            [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
                        wrap([$class: 'MaskPasswordsBuildWrapper']) {
                            sh "mvn -P runSonar -s $MAVEN_SETTINGS sonar:sonar -Dmaven.root=${env.WORKSPACE} -Dsonar.host.url=http://a34apvl00025.devillo.no:9000/sonarqube/ -Dsonar.login=${username} -Dsonar.password=${password}"
                        }
                    }
                }

                publishHTML(target: [
                        allowMissing         : true,
                        alwaysLinkToLastBuild: false,
                        keepAll              : true,
                        reportDir            : '/target/coverage',
                        reportFiles          : 'index.html',
                        reportName           : "Java CoCo"
                ])

            }
            
        } catch(error) {
            if (opt_deploy) {
                info(environment)
            }                

            throw error
        }       
    }
}

Object deployApp(app, version, environment, callback, reporter) {
    def envMap = [
            'ussi1': '22579', 't5': '16561'
    ]
    parsedEnvironment = envMap[environment]

    println("Init deploy with the following input")
    println("Application: \t ${app}")
    println("Version: \t ${version}")
    println("Environment: \t ${environment} (translated to: ${parsedEnvironment})")
    println("On behalf of: \t ${reporter}")
    println("Will callback on ${callback}")

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.SERVICE_USER, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
    
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