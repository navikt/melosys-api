import groovy.json.JsonSlurperClassic

timestamps {
	def username = ''
    def password = ''    
    def fasitCredentialId = ''
	def deploy = false
	def deployVersion = ''
    def skipUTests = '-DskipUTs'
    def skipITests = '-DskipITs'
    def miljo = ''
    
    node {

        try {
            env.LANG = "nb_NO.UTF-8"
            fasitCredentialId = env.FASIT_CRED
            info(fasitCredentialId)

            stage("Init") {
                printStage("Init")
                env.JAVA_HOME = "${tool 'jdk-1.8'}"
                env.PATH = "${tool 'maven-3.5.0'}/bin:${env.PATH}"
                
                checkout scm

                withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: fasitCredentialId,
                                  usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    username = env.USERNAME
                    password = env.PASSWORD
                    info(username)
					info(password)
                }
            }

            def artifactId = readFile('pom.xml') =~ '<artifactId>(.+)</artifactId>'
            artifactId = artifactId[0][1]

            if (deployVersion.isEmpty()) {
                def version = readFile('pom.xml') =~ '<version>(.+)</version>'
                pomVersion = version[0][1]
                deployVersion = pomVersion
            }

			stage("Build") {

				printStage("Build")
				configFileProvider(
						[configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
					sh 'mvn --batch-mode -V -U -e -s $MAVEN_SETTINGS clean deploy'
				}
                info("Build -- DONE")

			}
            
            if (deploy) {

                stage("Deploy") {
                    printStage("Deploy")

                    configFileProvider(
                            [configFile(fileId: 'navMavenSettings', variable: 'MAVEN_SETTINGS')]) {
                        wrap([$class: 'MaskPasswordsBuildWrapper']) {
                            sh 'mvn -s $MAVEN_SETTINGS -Denv=' + miljo + ' -Dapps=' + artifactId + ':' + deployVersion + ' -Dusername=' + username + ' -Dpassword=' + password + ' no.nav.maven.plugins:aura-maven-plugin:RELEASE:verify no.nav.maven.plugins:aura-maven-plugin:6.1.90:deploy'
                        }
                    }
                    def appUrl = getAppUrl(miljo, artifactId)
                    def ret = "<a href=" + appUrl + ">" + appUrl + "</a>"
                    info(miljo)

                }
            }            
            
		} catch(error) {

            emailext (
                    subject: "[AUTOMAIL] Feilet jobb ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                    body: "<p>Hei,<br><br>har du tid til å ta en titt på hva som kan være feil?<br>" +
                            "<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a><br><br>" +
                            "Tusen takk på forhånd,<br>Miljø</p>",
                    recipientProviders: [[$class: 'DevelopersRecipientProvider'],
                                         [$class: 'CulpritsRecipientProvider']]
            )
            throw error
        }		
	}
}

void info(msg) {
    ansiColor('xterm') {
        println "\033[45m\033[37m " + msg + " \033[0m"
    }
    currentBuild.description = msg
}

void printStage(stage) {
    ansiColor('xterm') {
        println "\033[46m Entered stage " + stage + " \033[0m"
    }
}