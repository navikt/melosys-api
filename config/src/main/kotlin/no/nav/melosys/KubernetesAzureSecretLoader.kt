package no.nav.melosys

import org.springframework.boot.SpringApplication
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Laster Azure client secret fra nais (plattform-styrt secret) når applikasjonen
 * kjører med local-q1 eller local-q2 profil.
 *
 * Se https://doc.nais.io/services/secrets/how-to/get-platform-secret/
 */
class KubernetesAzureSecretLoader : EnvironmentPostProcessor {

    companion object {
        private val LOCAL_Q_PROFILE = arrayOf("local-q2", "local-q1")
        private const val DEFAULT_SCRIPT_PATH = "scripts/get-azure-secrets.sh"
        private const val DEFAULT_ENVIRONMENT = "dev-fss"
        private const val DEFAULT_REASON = "Lokal utvikling av melosys-api"
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private const val CLASS_NAME = "no.nav.melosys.KubernetesAzureSecretLoader"
        private val IS_WINDOWS = System.getProperty("os.name").lowercase().contains("win")
    }

    private var applicationName = "melosys"

    private fun log(level: String, message: String) {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        println("$timestamp |  | $CLASS_NAME | $level | $message")
    }

    private fun logInfo(message: String) = log("INFO", message)

    private fun logWarn(message: String) = log("WARN", message)

    private fun logError(message: String) = log("ERROR", message)

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (LOCAL_Q_PROFILE.none { profile -> profile in environment.activeProfiles }) {
            return
        }

        if (environment.activeProfiles.contains("local-q1")) {
            applicationName = "melosys-q1"
        }

        val scriptPath = environment.getProperty("nais.azure.script.path")
            ?: environment.getProperty("kubernetes.azure.script.path", DEFAULT_SCRIPT_PATH)
        val naisEnvironment = environment.getProperty("nais.azure.environment", DEFAULT_ENVIRONMENT)
        val reason = environment.getProperty("nais.azure.reason", DEFAULT_REASON)
        val extraPaths = listOfNotNull(
            environment.getProperty("nais.cli.path"),
            environment.getProperty("nais.jq.path")
        ).filter { it.isNotBlank() }.distinct()

        applicationName = environment.getProperty("nais.azure.app-name", applicationName)

        try {
            logInfo("Henter AZURE_APP_CLIENT_SECRET for $applicationName i $naisEnvironment via nais CLI...")
            val clientSecret = executeShellScript(scriptPath, naisEnvironment, reason, extraPaths).trim()

            if (clientSecret.isNotBlank()) {
                applyClientSecret(environment, clientSecret)
                logInfo("Lastet inn AZURE_APP_CLIENT_SECRET")
            } else {
                logWarn("Tom AZURE_APP_CLIENT_SECRET returnert fra script")
            }
        } catch (e: Exception) {
            logError("Feilet med å hente AZURE_APP_CLIENT_SECRET: ${e.message}")
            logError("Sjekk at 'nais' og 'jq' er installert og at du er innlogget med 'nais login'.")
        }
    }

    private fun applyClientSecret(environment: ConfigurableEnvironment, clientSecret: String) {
        val properties = mapOf(
            "AZURE_APP_CLIENT_SECRET" to clientSecret,
            "azure.client.secret" to clientSecret,
            "spring.security.oauth2.client.registration.azure.client-secret" to clientSecret
        )

        val propertySource = MapPropertySource("azure-client-secret", properties)
        environment.propertySources.addFirst(propertySource)
        System.setProperty("AZURE_APP_CLIENT_SECRET", clientSecret)
        logInfo("Setter client secret: ${clientSecret.take(3)}...${clientSecret.takeLast(3)}")
    }

    private fun executeShellScript(
        scriptPath: String,
        naisEnvironment: String,
        reason: String,
        extraPaths: List<String>
    ): String {
        val scriptFile = File(System.getProperty("user.dir"), scriptPath)

        if (!scriptFile.exists()) {
            throw RuntimeException("Script ikke funnet: ${scriptFile.absolutePath}")
        }

        if (!scriptFile.canExecute()) {
            scriptFile.setExecutable(true)
        }

        val env = HashMap<String, String>(System.getenv())
        env["NAIS_SECRET_REASON"] = reason

        if (extraPaths.isNotEmpty()) {
            val pathSeparator = if (IS_WINDOWS) ";" else ":"
            env["PATH"] = extraPaths.joinToString(pathSeparator) + pathSeparator + (env["PATH"] ?: "")
            logInfo("La til ${extraPaths.joinToString(", ")} i PATH")
        }

        val command = if (IS_WINDOWS) {
            arrayOf("cmd.exe", "/c", scriptFile.absolutePath, applicationName, naisEnvironment)
        } else {
            val shell = System.getenv("SHELL") ?: "/bin/bash"
            arrayOf(shell, "-c", "\"${scriptFile.absolutePath}\" \"$applicationName\" \"$naisEnvironment\"")
        }

        val errorFile = File.createTempFile("azure-secret-", ".err").apply { deleteOnExit() }

        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment().putAll(env)
        processBuilder.redirectError(errorFile)

        val process = processBuilder.start()
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.forEachLine { output.append(it).append("\n") }
        }

        val exitCode = process.waitFor()
        val errorOutput = runCatching { errorFile.readText().trim() }.getOrDefault("")
        errorFile.delete()

        if (exitCode != 0) {
            val details = errorOutput.ifBlank { output.toString().trim().ifBlank { "(ingen output)" } }
            throw RuntimeException("Script feilet med exit code $exitCode:\n$details")
        }

        if (errorOutput.isNotBlank()) {
            logWarn(errorOutput)
        }

        return output.toString().trim()
    }
}
