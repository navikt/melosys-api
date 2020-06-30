#!groovy

//
// Forventer følgende build-parametere:
// - Miljo: Hvilket miljø (namespace) på NAIS som applikasjonen skal deployes til.

node {

    properties([
        parameters([
            choice(choices: ['--ingen--', 't8', 'q0', 'q1', 'q2', 'p'],
                description: 'Hvilket miljø skal applikasjon deployes til.', name: 'ENV')
        ])
    ])

    def KUBECTL = "/usr/local/bin/kubectl"
    def KUBECONFIG_NAISERATOR = "/var/lib/jenkins/kubeconfigs/kubeconfig-teammelosys.json"
    def NAISERATOR_CONFIG = "nais.yaml"
    def DEFAULT_BUILD_USER = "eessi2-jenkins"

    def cluster = "dev-fss"
    def dockerRepo = "repo.adeo.no:5443"
    def environment = "${params.ENV}".toString()
    def namespace
    def mvnSettings = "navRepoAdeoMavenSettings"
    def branchName, commit, commitId, imageVersion
    def application = "melosys", springProfiles = "nais"
    def javaHome = tool "jdk-11"
    def mvnHome = tool "maven-3.6.0"
    env.PATH = "${javaHome}/bin:${mvnHome}/bin:${env.PATH}"

    if (environment == 'p') {
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

        stage("Docker login") {
            withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "nexus-uploader", usernameVariable: "NEXUS_USERNAME", passwordVariable: "NEXUS_PASSWORD"]]) {
                sh "echo ${NEXUS_PASSWORD} | docker login -u ${NEXUS_USERNAME} --password-stdin ${dockerRepo}"
            }
        }

        stage("Checkout") {
            scmInfo = checkout scm

            branchName = resolveBranchName(scmInfo.GIT_BRANCH.toString())
            commitId = scmInfo.GIT_COMMIT

            commit = sh(script: "git log -1 --oneline", returnStdout: true)
            imageVersion = "${branchName}-${BUILD_NUMBER}-${commitId}"
            imageVersion = imageVersion.replaceAll("[æøåÆØÅ]", "x");
        }

        stage("Build application") {
            configFileProvider([configFile(fileId: "$mvnSettings", variable: "MAVEN_SETTINGS")]) {
                sh "mvn clean package -B -e -U -s $MAVEN_SETTINGS"
            }
        }

        if (environment == '--ingen--') {
            echo "Bygd OK uten deployment"
            currentBuild.result = "SUCCESS"
            return
        }

        stage("Build & publish Docker image") {
            sh "docker build --build-arg JAR_FILE=${application}-${imageVersion}.jar --build-arg SPRING_PROFILES=${springProfiles} -t ${dockerRepo}/melosys/${application}:${imageVersion} ."
            sh "docker push ${dockerRepo}/melosys/${application}:${imageVersion}"
        }

        stage("Deploy to NAIS") {
            prepareNaisYaml(NAISERATOR_CONFIG, imageVersion, namespace, cluster)

            sh "${KUBECTL} config --kubeconfig=${KUBECONFIG_NAISERATOR} set-context ${cluster} --namespace=${namespace}"
            sh "${KUBECTL} config --kubeconfig=${KUBECONFIG_NAISERATOR} use-context ${cluster}"
            sh "${KUBECTL} apply --kubeconfig=${KUBECONFIG_NAISERATOR} -f ${NAISERATOR_CONFIG}"
            sh "${KUBECTL} rollout status deployment/${application} --kubeconfig=${KUBECONFIG_NAISERATOR}"
        }

        if (namespace == 'q1' || namespace == 'p') {

            stage("Publish to Nexus") {
                configFileProvider([configFile(fileId: "$mvnSettings", variable: "MAVEN_SETTINGS")]) {
                    sh "mvn -DskipTests -DdeployAtEnd=true -DretryFailedDeploymentCount=5 --settings $MAVEN_SETTINGS deploy"
                }
            }
        }

        stage ("Send Slack-message") {
            GString message = ":clap: Siste commit på ${branchName} bygd og deployet OK til miljø ${environment}.\nCommit: ${commit}"
            sendSlackMessage("good", message)
        }

    } catch (e) {
        GString message = ":crying_cat_face: \n Siste commit på ${branchName} kunne ikke deployes til ${environment}. Se logg for mer info ${env.BUILD_URL}\nCommit ${commit}"
        sendSlackMessage("danger", message)
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

def prepareNaisYaml(naiseratorFile, imageVersion, namespace, cluster) {
    // set version in yaml-file:
    replaceInFile('@@IMAGE_VERSION@@', imageVersion, naiseratorFile)

    def domain
    if (cluster == "prod-fss") {
        domain = ".nais.adeo.no"
    } else {
        domain = ".nais.preprod.local"
    }

    if (namespace == "default") {
        replaceInFile('@@URL@@', 'melosys-api' + domain, naiseratorFile)
    } else {
        replaceInFile('@@URL@@', "melosys-api-" + namespace.toString() + domain, naiseratorFile)
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

def resolveBranchName(String branchName) {
    if (branchName.contains('/')) {
        split = branchName.split('/')
        branchName = split[split.length - 1]
    }

    return branchName
}
