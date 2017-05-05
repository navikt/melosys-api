import groovy.json.JsonSlurperClassic

timestamps {
	def deployVersion = ''
	
    node {

		properties([parameters([
                booleanParam(defaultValue: false, description: '', name: 'skip_UTests'),
                booleanParam(defaultValue: false, description: '', name: 'skip_ITests')
				])
        ])

        try {
            env.LANG = "nb_NO.UTF-8"

            stage("Init") {
                printStage("Init")
                env.JAVA_HOME = "${tool 'jdk-1.8'}"
                env.PATH = "${tool 'maven-3.5.0'}/bin:${env.PATH}"
                
                checkout scm

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

void printStage(stage) {
    ansiColor('xterm') {
        println "\033[46m Entered stage " + stage + " \033[0m"
    }
}