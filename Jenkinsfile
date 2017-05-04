import groovy.json.JsonSlurperClassic

timestamps {

		properties([parameters([
                booleanParam(defaultValue: false, description: '', name: 'skip_UTests'),
                booleanParam(defaultValue: false, description: '', name: 'skip_ITests'),
                [$class: 'CredentialsParameterDefinition', credentialType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl',
                 defaultValue: 'ade43d98-326c-41ad-9a61-aefbf933e5d2', description: '', name: 'fasitCred', required: true]
				 ])
        ])

        try {
            env.LANG = "nb_NO.UTF-8"

            stage("Init") {
                printStage("Init")
                env.JAVA_HOME = "${tool 'jdk-1.8'}"
                env.PATH = "${tool 'maven-3.5.0'}/bin:${env.PATH}"
                
                checkout scm

                withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: params.fasitCred,
                                  usernameVariable: 'SAVEDUSERNAME', passwordVariable: 'SAVEDPASSWORD']]) {
                    username = env.SAVEDUSERNAME
                    password = env.SAVEDPASSWORD
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

			}			
	
}

void printStage(stage) {
    ansiColor('xterm') {
        println "\033[46m Entered stage " + stage + " \033[0m"
    }
}