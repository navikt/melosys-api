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
 * Laster hemmeligheter fra nais (plattform-styrte secrets) når applikasjonen
 * kjører med local-q1 eller local-q2 profil.
 *
 * Se https://doc.nais.io/services/secrets/how-to/get-platform-secret/
 */
class KubernetesAzureSecretLoader : EnvironmentPostProcessor {

    companion object {
        private val LOCAL_Q_PROFILE = arrayOf("local-q2", "local-q1")
        private const val DEFAULT_SCRIPT_PATH = "scripts/get-azure-secrets.sh"
        private const val DEFAULT_ENVIRONMENT = "dev-fss"
        private const val DEFAULT_TEAM = "teammelosys"
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private val REASON_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        private const val CLASS_NAME = "no.nav.melosys.KubernetesAzureSecretLoader"
        private val IS_WINDOWS = System.getProperty("os.name").lowercase().contains("win")

        private const val CLIENT_ID = "AZURE_APP_CLIENT_ID"
        private const val CLIENT_SECRET = "AZURE_APP_CLIENT_SECRET"
        private const val DB_PASSWORD = "MELOSYSDB_PASSWORD"
        private const val SRV_USERNAME = "SRV_USERNAME"
        private const val SRV_PASSWORD = "SRV_PASSWORD"

        private val REQUESTED_VARIABLES = listOf(
            CLIENT_ID,
            CLIENT_SECRET,
            DB_PASSWORD,
            SRV_USERNAME,
            SRV_PASSWORD
        )

        /**
         * Ekstra property-navn som skal peke på samme verdi som miljøvariabelen.
         * AZURE_APP_*-variablene refereres direkte ved env-navn i application.yml
         * og trenger derfor ingen alias.
         *
         * MELOSYSDB_URL og MELOSYSDB_USERNAME hentes bevisst ikke. URL-en på nais
         * peker på en intern adresse som ikke er tilgjengelig lokalt, og brukernavnet
         * er ikke hemmelig. Begge er derfor hardkodet per profil i
         * application-local-q1/q2.yml.
         */
        private val PROPERTY_ALIASES = mapOf(
            DB_PASSWORD to listOf("melosysDB.password"),
            SRV_USERNAME to listOf("systemuser.username"),
            SRV_PASSWORD to listOf("systemuser.password")
        )

        /**
         * Begrunnelsen havner i nais sin audit-logg. Den skal gjøre det tydelig for
         * den som leser loggen hvorfor hemmeligheten ble lest ut, og hvor den tok veien.
         */
        private fun buildReason(profile: String, variables: List<String>): String {
            val tidspunkt = LocalDateTime.now().format(REASON_TIMESTAMP_FORMATTER)
            return "Lokal utvikling av melosys-api med profil $profile, startet $tidspunkt. " +
                "Verdiene (${variables.joinToString(", ")}) settes kun som miljøvariabler " +
                "i den lokale applikasjonsprosessen og lagres ikke på disk."
        }
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
        val team = environment.getProperty("nais.azure.team", DEFAULT_TEAM)
        val debug = environment.getProperty("nais.azure.debug", "false").toBoolean()
        val extraPaths = listOfNotNull(
            environment.getProperty("nais.cli.path"),
            environment.getProperty("nais.jq.path")
        ).filter { it.isNotBlank() }.distinct()

        applicationName = environment.getProperty("nais.azure.app-name", applicationName)

        // Variabler som allerede er satt i miljøet skal ikke overstyres
        val missing = REQUESTED_VARIABLES.filter { environment.getProperty(it).isNullOrBlank() }

        if (missing.isEmpty()) {
            logInfo("Alle nais-variabler er allerede satt, hopper over oppslag mot nais")
            return
        }

        try {
            val profile = LOCAL_Q_PROFILE.firstOrNull { it in environment.activeProfiles } ?: "local-q"
            val reason = environment.getProperty("nais.azure.reason") ?: buildReason(profile, missing)

            logInfo("Henter ${missing.joinToString(", ")} for $applicationName i $naisEnvironment via nais CLI...")
            val output = executeShellScript(scriptPath, naisEnvironment, team, reason, debug, missing, extraPaths)
            val values = parseOutput(output)

            if (values.isEmpty()) {
                logWarn("Ingen verdier returnert fra script")
                return
            }

            applyValues(environment, values)

            val notFound = missing - values.keys
            if (notFound.isNotEmpty()) {
                logWarn("Fant ikke: ${notFound.joinToString(", ")}. Kjør scriptet med --list for å se tilgjengelige variabler.")
            }
        } catch (e: Exception) {
            logError("Feilet med å hente hemmeligheter fra nais: ${e.message}")
            logError("Sjekk at 'nais' og 'jq' er installert og at du er innlogget med 'nais login'.")
        }
    }

    /** Scriptet skriver én "NAVN=verdi" per linje når flere variabler etterspørres. */
    private fun parseOutput(output: String): Map<String, String> =
        output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('=') }
            .map { line -> line.substringBefore('=') to line.substringAfter('=') }
            .filter { (name, value) -> name.isNotBlank() && value.isNotBlank() }
            .toMap()

    private fun applyValues(environment: ConfigurableEnvironment, values: Map<String, String>) {
        val properties = mutableMapOf<String, Any>()

        values.forEach { (name, value) ->
            properties[name] = value
            PROPERTY_ALIASES[name]?.forEach { alias -> properties[alias] = value }
            System.setProperty(name, value)
        }

        environment.propertySources.addFirst(MapPropertySource("nais-secrets", properties))
        logInfo("Lastet inn: ${values.keys.joinToString(", ")}")
    }

    private fun executeShellScript(
        scriptPath: String,
        naisEnvironment: String,
        team: String,
        reason: String,
        debug: Boolean,
        variables: List<String>,
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

        val arguments = buildList {
            add("--app"); add(applicationName)
            add("--environment"); add(naisEnvironment)
            add("--team"); add(team)
            if (debug) add("--debug")
            add("--")
            addAll(variables)
        }

        // Kjør scriptet direkte med argumenter, ikke via `shell -c` med innlimt streng.
        val command = if (IS_WINDOWS) {
            arrayOf("cmd.exe", "/c", scriptFile.absolutePath) + arguments
        } else {
            arrayOf(scriptFile.absolutePath) + arguments
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

        // stdout inneholder hemmelighetene og må ALDRI logges eller pakkes inn i en exception.
        // Kun stderr fra scriptet er trygt å vise; det inneholder bare navn og feilmeldinger.
        val result = output.toString().trim()

        if (exitCode != 0) {
            if (errorOutput.isNotBlank()) {
                logWarn(errorOutput)
            }
            // Scriptet returnerer exit 1 også ved delvis suksess. Har vi fått noe brukbart,
            // bruker vi det og lar kallende kode rapportere hva som manglet.
            if (result.isBlank()) {
                throw RuntimeException("Script feilet med exit code $exitCode. Se meldingene over.")
            }
            logWarn("Script feilet med exit code $exitCode, men noen verdier ble hentet")
            return result
        }

        if (errorOutput.isNotBlank()) {
            logWarn(errorOutput)
        }

        return result
    }
}
