package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class MigreringsRapport(private val environment: Environment) {

    @Volatile
    internal var antallSakerFunnet: Int = 0

    @Volatile
    internal var antallSakerErRedigerbar: Int = 0

    @Volatile
    internal var antallSakerProssessert: Int = 0

    @Volatile
    internal var antallSakerMigrert: Int = 0

    @Volatile
    internal var antallOppgaverLaget: Int = 0

    @Volatile
    internal var alleredeMigrert: Int = 0

    @Volatile
    internal var migreringFeilet: Int = 0

    @Volatile
    internal var harIkkeGyldigKombinasjon: Int = 0

    @Volatile
    private var sakSomMangerOppgaveAntall: Int = 0

    private val sakerManglerOppgave = mutableListOf<String>()
    private val sakerMedFlereOppgaver = mutableListOf<String>()
    private val sakHvorViSkalHaSedMenSomIkkeFinnes = mutableListOf<String>()
    private val sakHvorMappingFeiler = mutableListOf<String>()
    private val migreringsSakListe = mutableListOf<MigreringsSak>()

    internal fun sortedMigreringsListe() = migreringsSakListe.sortedWith(
        compareBy(
            { it.sak.sakstype },
            { it.sak.sakstema },
            { it.sak.behandlingstype },
            { it.sak.behandlingstema },
            { it.sak.behandlingstatus },
        )
    )

    fun status(): Map<String, Any> {
        return mapOf(
            "antallSakerFunnet" to antallSakerFunnet,
            "antallSakerErRedigerbar" to antallSakerErRedigerbar,
            "antallSakerProssessert" to antallSakerProssessert,
            "antallSakerMigrert" to antallSakerMigrert,
            "antallOppgaverLaget" to antallOppgaverLaget,
            "alleredeMigrert" to alleredeMigrert,
            "migreringFeilet" to migreringFeilet,
            "harIkkeGyldigKombinasjon" to harIkkeGyldigKombinasjon,
            "harIkkeÅpenOppgave" to sakSomMangerOppgaveAntall,
        )
    }

    internal fun mappingFeiler(message: String) {
        sakHvorMappingFeiler.add(message)
    }

    internal fun migrertSak(migreringsSak: MigreringsSak) {
        migreringsSakListe.add(migreringsSak)
    }

    internal fun sakManglerOppgave(saksnummer: String) {
        sakSomMangerOppgaveAntall++
        sakerManglerOppgave.add(saksnummer)
    }

    internal fun sakerMedFlereOppgaver(message: String) {
        log.warn(message)
        sakerMedFlereOppgaver.add(message)
    }

    internal fun finnesIkkeSedForSak(msg: String) {
        sakHvorViSkalHaSedMenSomIkkeFinnes.add(msg)
    }

    internal fun statusEtterKjøring(): String {
        return status().toJsonNode.toPrettyString()
    }

    internal fun saveStatusFiles(status: String) {
        val profil = environment.activeProfiles.firstOrNull() ?: "local-test"
        log.info("Profile:$profil")
        if (profil !in listOf(
                "local-test",
                "local-mock",
                "local-q1",
                "local-q2"
            )
        ) return // kun lag profiler ved lokal kjøring

        val localOutputFolder = System.getenv("lokal-output-folder") ?: return
        val timeForRun =
            "$localOutputFolder/$profil-${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
            }"
        File(timeForRun).mkdirs()

        File("$timeForRun/rapport.json").writeText(migreringsSakListeSomJsonString())
        File("$timeForRun/status.json").writeText(status)
    }

    fun migreringsSakListeSomJsonString(): String = migreringsSakListe.toJsonNode.toPrettyString()

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
