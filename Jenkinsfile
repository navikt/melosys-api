timestamps {
    def username = ''
    def password = ''    
    def fasitCredentialId = ''
    def build = false
    def deploy = false
    def deployVersion = ''
    def skipUTests = '-DskipUTs'
    def skipITests = '-DskipITs'
    def environment = ''
    
    try {
        build = Boolean.valueOf(BUILD)
        deploy = Boolean.valueOf(DEPLOY)
        fasitCredentialId = env.FASIT_CRED
        if (env.DEPLOY_VERSION != null) {
            deployVersion = env.DEPLOY_VERSION
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

            stage("Init") {
                printStage("Init")
                env.JAVA_HOME = "${tool 'jdk-1.8'}"
                env.PATH = "${tool 'maven-3.5.0'}/bin:${env.PATH}"
                
                checkout scm

                withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: fasitCredentialId,
                                  usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    username = env.USERNAME
                    password = env.PASSWORD
                }
            }
            
            def artifactId = readFile('pom.xml') =~ '<artifactId>(.+)</artifactId>'
            artifactId = artifactId[0][1]

            if (deployVersion.isEmpty()) {
                def version = readFile('pom.xml') =~ '<version>(.+)</version>'
                pomVersion = version[0][1]
                deployVersion = pomVersion
            }           

            if (build) {
                stage("Build") {
                    printStage("Build")
                    configFileProvider(
                        [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn --batch-mode -V -U -e -s $MAVEN_SETTINGS clean deploy'
                    }
                }            

                info("Build ${artifactId}:${deployVersion}")
            }            
            
            if (deploy) {

                stage("Deploy") {
                    printStage("Deploy")

                    configFileProvider(
                            [configFile(fileId: 'navMavenSettings', variable: 'MAVEN_SETTINGS')]) {
                        wrap([$class: 'MaskPasswordsBuildWrapper']) {
                            sh 'mvn --batch-mode -V -U -e -s $MAVEN_SETTINGS -Denv=' + environment + ' -Dapps=' + artifactId + ':' + deployVersion + ' -Dusername=' + username 
                                + ' -Dpassword=' + password + ' no.nav.maven.plugins:aura-maven-plugin:RELEASE:verify no.nav.maven.plugins:aura-maven-plugin:6.1.90:deploy'
                        }
                    }

                    info("Deploy ${artifactId}:${deployVersion} to ${environment}")

                }
            }            
            
        } catch(error) {
            if (deploy) {
                info(environment)
            }                

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