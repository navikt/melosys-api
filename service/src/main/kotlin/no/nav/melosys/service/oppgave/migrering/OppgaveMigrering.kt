package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.BehandlingRepositoryForOppgaveMigrering
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OppgaveMigrering(
    private val behandlingRepository: BehandlingRepositoryForOppgaveMigrering,
    private val oppgaveFasade: OppgaveFasade,
    private val environment: Environment
) {

    private val nyOppgaveFactory = OppgaveFactory(FakeUnleash().apply {
        enable(ToggleName.NY_GOSYS_MAPPING)
    })

    private val sakerManglerOppgave = mutableListOf<String>()
    private val sakerMedOppgave = mutableListOf<String>()
    private val sakerMedFlereOppgaver = mutableListOf<String>()
    private val sakHvorViSkalHaSedMenSomIkkeFinnes = mutableListOf<String>()
    private val sakHvorMappingFeiler = mutableListOf<String>()
    val migreringsSakListe = mutableListOf<MigreringsSak>()

    @Volatile
    private var antallSakerFunnet: Int = 0

    @Volatile
    private var antallSakerErRedigerbar: Int = 0

    @Volatile
    private var antallSakerProssessert: Int = 0

    @Volatile
    private var antallSakerMigrert: Int = 0

    @Volatile
    private var harIkkeÅpenOppgave: Int = 0

    fun status(): Map<String, Any> {
        return mapOf(
            "antallSakerFunnet" to antallSakerFunnet,
            "antallSakerErRedigerbar" to antallSakerErRedigerbar,
            "antallSakerProssessert" to antallSakerProssessert,
            "antallSakerMigrert" to antallSakerMigrert,
            "harIkkeÅpenOppgave" to harIkkeÅpenOppgave,
        )
    }

    private val behandlingsstatuser = listOf(
        Behandlingsstatus.AVSLUTTET,
        Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
        Behandlingsstatus.IVERKSETTER_VEDTAK,
    )

    @Async
    @Synchronized
    fun go(bruker: String?, saksnummer: String?, dryrun: Boolean) {
        ThreadLocalAccessInfo.executeProcess("Prossess oppgaver") {
            migrering(bruker, saksnummer, dryrun)
        }
    }

    private fun finnSaker(
        bruker: String?,
        saksnummer: String?,
        dryrun: Boolean
    ): MutableCollection<SakOgBehandlingDTO> {
        if (saksnummer != null) {
            return behandlingRepository.finnSak(saksnummer, behandlingsstatuser)
        }
        if (bruker != null) {
            return behandlingRepository.finnSakerRegistrertAv(bruker, behandlingsstatuser)
        }
        return behandlingRepository.finnSaksOgBehandlingTyperOgTema(behandlingsstatuser)
    }

    private fun migrering(bruker: String?, saksnummer: String?, dryrun: Boolean) {
        log.info("Utfører OppgaveMigrering")
        finnSaker(bruker, saksnummer, dryrun).apply {
            log.info("size før erRedigerbar: $size")
            antallSakerFunnet = size
        }.filter { it.erRedigerbar() }.sortedBy { it.saksnummer }.apply {
            antallSakerErRedigerbar = size
        }.forEach { sak ->
            antallSakerProssessert++
            oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(sak.saksnummer).apply {
                // TODO: legg til oppgaveFasade.oppdaterOppgave()
                val migreringsSak = MigreringsSak(sak, this, nyOppgaveMapping(sak))
                migreringsSakListe.add(migreringsSak)
                if (size == 0) {
                    harIkkeÅpenOppgave++
                    sakerManglerOppgave.add(sak.saksnummer)
                }
                if (size > 1) {
                    log.warn("fant $size for: ${sak.saksnummer}")
                    sakerMedFlereOppgaver.add("fant $size oppgaver for: ${sak.saksnummer}")
                }
                if (size == 1) {
                    sakerMedOppgave.add(sak.saksnummer)
                    antallSakerMigrert++
                }
            }
        }
        log.info("OppgaveMigrering utført!")
        lagRapport()
    }

    private fun nyOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO): OppgaveOppdatering {
        try {
            val oppgaveBehandlingstema: OppgaveBehandlingstema? = sakOgBehandling.utledOppgaveBehandlingstema()
            val oppgavetype: Oppgavetyper = sakOgBehandling.utledOppgaveType()
            val beskrivelse: String = sakOgBehandling.utledBeskrivelse(oppgaveBehandlingstema)
            val tema: Tema = sakOgBehandling.utledTema()
            return OppgaveOppdatering(oppgaveBehandlingstema, null, tema, oppgavetype, beskrivelse)
        } catch (e: Exception) {
            val gyldige = GyldigeKombinasjoner.finnGyldige(
                sakOgBehandling.sakstype,
                sakOgBehandling.sakstema,
                sakOgBehandling.behandlingstype,
                sakOgBehandling.behandlingstema
            )
            val mappingError = "${sakOgBehandling.saksnummer}: gyldige:${gyldige.size}  ${e.message}"
            sakHvorMappingFeiler.add(mappingError)
            return OppgaveOppdatering(mappingError)
        }
    }

    private fun SakOgBehandlingDTO.utledOppgaveBehandlingstema(): OppgaveBehandlingstema? =
        nyOppgaveFactory.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private fun SakOgBehandlingDTO.utledOppgaveType(): Oppgavetyper =
        nyOppgaveFactory.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)

    private fun SakOgBehandlingDTO.utledTema(): Tema =
        nyOppgaveFactory.utledTema(
            sakstype, sakstema, behandlingstema
        )

    private fun SakOgBehandlingDTO.utledBeskrivelse(oppgaveBehandlingstema: OppgaveBehandlingstema?): String {
        val hentSedDokument = {
            log.info("Henter sed dokuemnt for: $behandlingID")
            sedDokument(behandlingID)
        }
        return try {
            nyOppgaveFactory.utledBeskrivelse(
                oppgaveBehandlingstema,
                sakstype,
                sakstema,
                behandlingstema,
                behandlingstype, hentSedDokument
            )
        } catch (e: Exception) {
            val message = e.message ?: "utledBeskrivelse feilet "
            val msg = "$message feilet for $saksnummer, behandlingID:$behandlingID"
            sakHvorViSkalHaSedMenSomIkkeFinnes.add(msg)
            return msg
        }
    }

    private fun sedDokument(behandlingID: Long): SedDokument? =
        hentBehandlingMedSaksoplysninger(behandlingID)
            .finnSedDokument().orElse(null)

    private fun hentBehandlingMedSaksoplysninger(behandlingID: Long): Behandling = behandlingRepository
        .findWithSaksopplysningerById(behandlingID) ?: throw TekniskException("Fant ikke behandling for $behandlingID")

    private fun lagRapport() {
        val status = statusEtterKjøring()
        log.info(status)
        saveStatusFiles(status)
    }

    private fun statusEtterKjøring(): String {
        val sb = StringBuilder()
        sb.appendLine()
        sb.appendLine("sakerMedOppgave: ${sakerMedOppgave.size}")
        sb.appendLine("sakerManglerOppgave: ${sakerManglerOppgave.size}")
        sb.appendLine("sakerMedFlereOppgaver: ${sakerMedFlereOppgaver.size}")
        sb.appendLine("sakHvorMappingFeiler: ${sakHvorMappingFeiler.size}")
        sb.appendLine("sakHvorViSkalHaSedMenSomIkkeFinnes: ${sakHvorViSkalHaSedMenSomIkkeFinnes.size}")
        return sb.toString()
    }

    private fun saveStatusFiles(status: String) {
        val profil = environment.activeProfiles.first()
        log.info("Profile:$profil")
        if (profil !in listOf("local-mock", "local-q1", "local-q2")) return // kun lag profiler ved lokal kjøring
        val timeForRun =
            "oppgave-migrering/$profil-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}"
        File(timeForRun).mkdirs()


        File("$timeForRun/diff.json").writeText(migreringsSakListe.toJsonNode.toPrettyString())

        File("$timeForRun/status.txt").writeText(status)
        File("$timeForRun/saker-med-oppgave.txt").writeText(sakerMedOppgave.joinToString(","))
        File("$timeForRun/saker-mangler-oppgave.txt").writeText(sakerManglerOppgave.joinToString(","))
        File("$timeForRun/saker-med-flere-oppgave.txt").writeText(sakerMedFlereOppgaver.joinToString("\n"))
        File("$timeForRun/sak-hvor-mapping-feiler.txt").writeText(sakHvorMappingFeiler.joinToString("\n"))
        File("$timeForRun/sak-hvor-vi-skal-ha-sed-men-som-ikke-finnes.txt").writeText(
            sakHvorViSkalHaSedMenSomIkkeFinnes.joinToString("\n")
        )
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
