package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.oppgave.Oppgave
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OppgaveIdMigreringRapport(
    private val environment: Environment,
    private val oppgaveIdMigrering: OppgaveIdMigrering
) {

    @Volatile
    internal var antallOppgaverFunnet: Int = 0

    @Volatile
    internal var antallSakerProssessert: Int = 0

    @Volatile
    internal var antallSakerMigrert: Int = 0

    @Volatile
    internal var alleredeMigrert: Int = 0


    private val oppgaveIdMigrert = mutableListOf<String>()
    private val oppgaveMigrert = mutableListOf<OppgaveMigrert>()
    private val sakerMedFlereÅpneBehandlinger = mutableListOf<String>()

    fun status(): Map<String, Any> {
        return mapOf(
            "antallOppgaverFunnet" to antallOppgaverFunnet,
            "antallSakerProssessert" to antallSakerProssessert,
            "antallSakerMigrert" to antallSakerMigrert,
            "alleredeMigrert" to alleredeMigrert,
            "antallOppgaverHvorMappingFeiler" to oppgaveMigrert.count { it.saksnummer == null || it.behandlingId == null },
            )
    }

    fun harOppgaveIdBlittMigrert(oppgaveId: String): Boolean {
        return oppgaveIdMigrert.contains(oppgaveId)
    }

    internal fun oppgaveMigrert(oppgave: Oppgave, saksnummer: String?, behandlingId: Long?, sakHarFlereÅpneBehandlinger: Boolean = false) {
        oppgaveIdMigrert.add(oppgave.oppgaveId)
        oppgaveMigrert.add(OppgaveMigrert(saksnummer, behandlingId, oppgave, sakHarFlereÅpneBehandlinger))
        antallSakerProssessert++
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

        File("$timeForRun/rapport.json").writeText(migreringsOppgaveMigrertListeSomJsonString())
        File("$timeForRun/status.json").writeText(status)
    }

    fun migreringsOppgaveMigrertListeSomJsonString(): String = oppgaveMigrert.toJsonNode.toPrettyString()

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}

data class OppgaveMigrert(
    val saksnummer: String?,
    val behandlingId: Long?,
    val oppgave: Oppgave,
    val sakHarFlereÅpneBehandlinger: Boolean = false
)
