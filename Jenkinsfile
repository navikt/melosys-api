#!groovy

//
// Forventer følgende build-parametere:
// - Miljo: Hvilket miljø (namespace) på NAIS som applikasjonen skal deployes til.

node {

    properties([
        parameters([
            choice(choices: ['t8', 'q1', 'prod'],
                description: 'Hvilket miljø skal applikasjon deployes til.', name: 'ENV')
        ])
    ])

    def KUBECTL = "/usr/local/bin/kubectl"
    def KUBECONFIG_NAISERATOR = "/var/lib/jenkins/kubeconfigs/kubeconfig-teammelosys.json"
    def NAISERATOR_CONFIG = "nais.yaml"
    def VERA_UPDATE_URL = "https://vera.adeo.no/api/v1/deploylog"
    def DEFAULT_BUILD_USER = "eessi2-jenkins"

    def cluster = "dev-fss"
    def dockerRepo = "docker.adeo.no:5000/melosys"
    def environment = "${params.ENV}".toString()
    def namespace
    def mvnSettings = "navMavenSettingsUtenProxy"
    def branchName, commit, releaseVersion, isSnapshot, imageVersion
    def application = "melosys", springProfiles = "nais"

    if (environment == 'prod') {
        namespace = 'default'
        cluster = 'prod-fss'
    } else if (environment == 'q1') {
        namespace = 'default'
        springProfiles += ",test"
    } else {
        namespace = environment
        springProfiles += ",test"
    }

    try {
        stage("Checkout") {
            scmInfo = checkout scm

            branchName = scmInfo.GIT_LOCAL_BRANCH

            commit = sh(script: "git log -1 --oneline", returnStdout: true)
            currentVersion = readMavenPom().version
            isSnapshot = currentVersion.contains("-SNAPSHOT")
            releaseVersion = currentVersion.tokenize("-")[0]
            timeStamp = new Date().format("YYYYMMddHHmmss")
            imageVersion = releaseVersion + "-" + timeStamp
        }

        stage("Build application") {
            configFileProvider([configFile(fileId: "$mvnSettings", variable: "MAVEN_SETTINGS")]) {
                sh "mvn versions:set -B -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false -s $MAVEN_SETTINGS"
                sh "mvn clean package -B -e -U -s $MAVEN_SETTINGS"
            }
        }

        stage("Build & publish Docker image") {

            configFileProvider([configFile(fileId: "$mvnSettings", variable: "MAVEN_SETTINGS")]) {
                sh "mvn clean package -DskipTests -B -s $MAVEN_SETTINGS"
                sh "docker build --build-arg JAR_FILE=${application}-${imageVersion}.jar --build-arg SPRING_PROFILES=${springProfiles} -t ${dockerRepo}/${application}:${imageVersion} --rm=true ."
                sh "docker push ${dockerRepo}/${application}:${imageVersion}"
            }
        }

        stage("Deploy to NAIS") {
            prepareNaisYaml(NAISERATOR_CONFIG, imageVersion, namespace, cluster)

            sh "${KUBECTL} config --kubeconfig=${KUBECONFIG_NAISERATOR} set-context ${cluster} --namespace=${namespace}"
            sh "${KUBECTL} apply --kubeconfig=${KUBECONFIG_NAISERATOR} -f ${NAISERATOR_CONFIG}"

            try {
                def deployer = getBuildUser(DEFAULT_BUILD_USER)
                sh "curl -i -s --header \"Content-Type: application/json\" --request POST --data \'{\"environment\": \"${namespace}\",\"application\": \"${application}\",\"version\": \"${imageVersion}\",\"deployedBy\": \"${deployer}\"}\' ${VERA_UPDATE_URL}"
            } catch (e) {
                println("[ERROR] Feil ved oppdatering av Vera. Exception: " + e)
            }
        }

        stage("New dev version") {
            if (branchName == "develop") {
                def nextVersion = resolveNextSnapshotVersionFrom(releaseVersion)
                setVersion(nextVersion)

                sh "git config user.name srvEESSI2"
                sh "git config user.email srvEESSI2@nav.no"
                sh "git commit -am \"Updated to snapshot version ${nextVersion} \""

                sshagent([BITBUCKET_SSH]) {
                    sh "git push origin HEAD:develop"
                }

                GString message = ":clap: Siste commit på ${branch} bygd og deployet OK.\nCommit: ${commit}"
                sendSlackMessage("good", message)
            }
        }
    } catch (e) {
        println("[ERROR] " + e)
        throw e
    }
}

def getBuildUser(defaultUser) {
    def buildUser = defaultUser

    try {
        wrap([$class: 'BuildUser']) {
            buildUser = "${BUILD_USER} (${BUILD_USER_ID})"
        }
    } catch (e) {
        // Dersom bygg er auto-trigget, er ikke BUILD_USER variablene satt => defaultUser benyttes
        return buildUser
    }
}

def prepareNaisYaml(naiseratorFile, version, namespace, cluster) {
    // set version in yaml-file:
    replaceInFile('@@RELEASE_VERSION@@', version, naiseratorFile)

    def domain
    if (cluster == "prod-fss") {
        domain = ".nais.adeo.no"
    } else {
        domain = ".nais.preprod.local"
    }

    if (namespace == "default") {
        replaceInFile('@@URL@@', 'melosys' + domain, naiseratorFile)
    } else {
        replaceInFile('@@URL@@', "melosys-" + namespace.toString() + domain, naiseratorFile)
    }

    replaceInFile('@@NAMESPACE@@', namespace, naiseratorFile)
}

def replaceInFile(oldString, newString, file) {
    sh "sed -i -e 's/${oldString}/${newString}/g' ${file}"
}

def sendSlackMessage(String color, String message) {

    try {
        slackSend color: color, message: message, tokenCredentialId: "melosys-slack-token"
    } catch (Exception exception) {
        echo("Failed to send message to Slack: ${exception.getMessage()}")
    }
}

def setVersion(String versionNumber) {
    configFileProvider([configFile(fileId: "navMavenSettingsUtenProxy", variable: "MAVEN_SETTINGS")]) {
        sh "mvn org.codehaus.mojo:versions-maven-plugin:2.5:set -B -DnewVersion=${versionNumber} -DgenerateBackupPoms=false --settings $MAVEN_SETTINGS"
    }

    currentBuild.displayName = "#${env.BUILD_ID} - $versionNumber"
}

def resolveNextSnapshotVersionFrom(String currentReleaseVersion) {
    def newSnapshotVersionComponents = currentReleaseVersion.tokenize(".")
    newSnapshotVersionComponents[1] = (newSnapshotVersionComponents[1] as Integer) + 1
    newSnapshotVersionComponents[2] = "0"
    return newSnapshotVersionComponents.join(".") + "-SNAPSHOT"
}