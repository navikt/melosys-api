package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OppgaveMigrering(
    private val behandlingRepository: BehandlingRepository,
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
    private val oppgaveMappingKjørelog = StringBuilder()


    fun go() {
        ThreadLocalAccessInfo.executeProcess("Prossess oppgaver") {
            migrering()
        }
    }

    private fun migrering() {
        log.info("Utfører OppgaveMigrering")
        val sakOgBehandlinger = behandlingRepository.findSaksOgBehandlingTyperOgTeam(
            listOf(
                Behandlingsstatus.AVSLUTTET,
                Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
                Behandlingsstatus.IVERKSETTER_VEDTAK,
            )
        ).apply {
            log.info("size før erRedigerbar: $size")
        }.filter { it.erRedigerbar() }.sortedBy { it.saksnummer }

        println("sakOgBehandlinger filtrert: ${sakOgBehandlinger.size}")

        sakOgBehandlinger.forEach {
            oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(it.saksnummer).apply {
                if (size > 1) sakMedFlereOppgaver(it)
                if (size == 0) sakManglerOppgave(it)
            }.firstOrNull()?.apply {
                gammelOppgaveMapping(it)
                nyOppgaveMapping(it)
            }
        }
        lagKjøreRapport()
    }

    private fun lagKjøreRapport() {
        val status = statusEtterKjøring()
        println(status)
        saveStatusFiles(status)
    }

    private fun statusEtterKjøring(): String {
        val sb = StringBuilder()
        sb.appendLine("sakerMedOppgave:                       ${sakerMedOppgave.size}")
        sb.appendLine("sakerManglerOppgave:                   ${sakerManglerOppgave.size}")
        sb.appendLine("sakerMedFlereOppgaver:                 ${sakerMedFlereOppgaver.size}")
        sb.appendLine("sakHvorMappingFeiler:                  ${sakHvorMappingFeiler.size}")
        sb.appendLine("sakHvorViSkalHaSedMenSomIkkeFinnes:    ${sakHvorViSkalHaSedMenSomIkkeFinnes.size}")
        return sb.toString()
    }

    private fun saveStatusFiles(status: String) {
        val envName = environment.getActiveProfiles().first()
        if (envName == "test") return
        val timeForRun = "oppgave-migrering/$envName-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}"
        File(timeForRun).mkdirs()

        File("$timeForRun/status.txt").writeText(status)
        File("$timeForRun/oppgaver.txt").writeText(oppgaveMappingKjørelog.toString())
        File("$timeForRun/saker-med-oppgave.txt").writeText(sakerMedOppgave.joinToString(","))
        File("$timeForRun/saker-mangler-oppgave.txt").writeText(sakerManglerOppgave.joinToString(","))
        File("$timeForRun/saker-med-flere-oppgave.txt").writeText(sakerMedFlereOppgaver.joinToString("\n"))
        File("$timeForRun/sak-hvor-mapping-feiler.txt").writeText(sakHvorMappingFeiler.joinToString("\n"))
        File("$timeForRun/sak-hvor-vi-skal-ha-sed-men-som-ikke-finnes.txt").writeText(
            sakHvorViSkalHaSedMenSomIkkeFinnes.joinToString("\n")
        )

    }

    private fun MutableList<Oppgave>.sakMedFlereOppgaver(sakOgBehandling: SakOgBehandlingDTO) {
        log.error("fant $size for: ${sakOgBehandling.saksnummer}")
        sakerMedFlereOppgaver.add("fant $size oppgaver for: ${sakOgBehandling.saksnummer}")
    }

    private fun Oppgave.gammelOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO) {
        log.info(sakOgBehandling.toString())
        log.info("oppgave:$oppgaveId - beskrivelse:${beskrivelse ?: ""}")
        sakerMedOppgave.add(sakOgBehandling.saksnummer)
        oppgaveMappingKjørelog.appendLine()
        oppgaveMappingKjørelog.appendLine("$sakOgBehandling")
        oppgaveMappingKjørelog.appendLine("oppgaveId=         $oppgaveId")
        oppgaveMappingKjørelog.appendLine("=========== gammel ===========")
        oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
        oppgaveMappingKjørelog.appendLine("tema=              $tema")
        oppgaveMappingKjørelog.appendLine("behandlingstema=   $behandlingstema")
        oppgaveMappingKjørelog.appendLine("behandlingstype=   $behandlingstype")
        oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
        oppgaveMappingKjørelog.appendLine("beskrivelse=       $beskrivelse")
        oppgaveMappingKjørelog.appendLine("=========== nye ===========")
    }

    private fun nyOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO) {
        try {
            val oppgaveBehandlingstema: OppgaveBehandlingstema = sakOgBehandling.utledOppgaveBehandlingstema()
            val oppgavetype: Oppgavetyper = sakOgBehandling.utledOppgaveType()
            val beskrivelse: String = sakOgBehandling.utledBeskrivelse(oppgaveBehandlingstema)
            val tema: Tema = sakOgBehandling.utledTema()
            oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
            oppgaveMappingKjørelog.appendLine("tema=              $tema")
            oppgaveMappingKjørelog.appendLine("behandlingstema=   ${oppgaveBehandlingstema.kode}")
            oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
            oppgaveMappingKjørelog.appendLine("beskrivelse=       $beskrivelse")
            oppgaveMappingKjørelog.appendLine("------------------------------------")
        } catch (e: Exception) {
            sakHvorMappingFeiler.add("${sakOgBehandling.saksnummer}: ${e.message}")
        }
    }

    private fun sakManglerOppgave(sakOgBehandlingDTO: SakOgBehandlingDTO) {
        log.warn("${sakOgBehandlingDTO.saksnummer} har ikke åpen oppgagave")
        sakerManglerOppgave.add(sakOgBehandlingDTO.saksnummer)
    }

    fun SakOgBehandlingDTO.utledOppgaveBehandlingstema(): OppgaveBehandlingstema =
        nyOppgaveFactory.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private fun SakOgBehandlingDTO.utledOppgaveType(): Oppgavetyper =
        nyOppgaveFactory.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)

    fun SakOgBehandlingDTO.utledTema(): Tema =
        nyOppgaveFactory.utledTema(
            sakstype, sakstema, behandlingstema
        )

    fun SakOgBehandlingDTO.utledBeskrivelse(oppgaveBehandlingstema: OppgaveBehandlingstema): String {
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
}

