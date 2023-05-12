package no.nav.melosys.service.oppgave.migrering

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
import no.nav.melosys.service.oppgave.OppgaveGosysMapping.Companion.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OppgaveMigrering(
    private val behandlingRepository: BehandlingRepositoryForOppgaveMigrering,
    private val oppgaveFasade: OppgaveFasade,
    private val migreringsRapport: MigreringsRapport,
) {

    private val nyOppgaveFactory = OppgaveFactory(FakeUnleash().apply {
        //  litt spesiel måte å bruke unleash på, men siden unleash skal vekk fra
        //  OppgaveFactory etter at vi er over på ny mapping så blir dette den enkleste løsningen
        enable(ToggleName.NY_GOSYS_MAPPING, NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING)
    })

    fun status(): Map<String, Any> {
        return migreringsRapport.status()
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
            migreringsRapport.antallSakerFunnet = size
        }.filter { it.erRedigerbar() }.sortedBy { it.saksnummer }.apply {
            migreringsRapport.antallSakerErRedigerbar = size
        }.forEach { sak ->
            migreringsRapport.antallSakerProssessert++
            oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(sak.saksnummer).let { oppgaver ->
                // TODO: legg til oppgaveFasade.oppdaterOppgave()
                val migreringsSak = MigreringsSak(sak, oppgaver, nyOppgaveMapping(sak))
                migreringsRapport.migrertSak(migreringsSak)
                if (oppgaver.size == 0) {
                    migreringsRapport.sakManglerOppgave(sak.saksnummer)
                }
                if (oppgaver.size > 1) {
                    migreringsRapport.sakerMedFlereOppgaver("fant ${oppgaver.size} oppgaver for: ${sak.saksnummer}")
                }
                if (oppgaver.size == 1) {
                    migreringsRapport.sakMedOppgave(migreringsSak)
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
            migreringsRapport.mappingFeiler(mappingError)
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
        val hentSedDokument = { logHvisMangler: Boolean ->
            log.info("Henter sed dokuemnt for: $behandlingID")
            sedDokument(behandlingID).apply {
                if (logHvisMangler && this == null) log.warn("Sed dokument mangler for:${saksnummer} behandlingID:${behandlingID}")
            }
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
            migreringsRapport.finnesIkkeSedForSak(msg)
            return msg
        }
    }

    private fun sedDokument(behandlingID: Long): SedDokument? =
        hentBehandlingMedSaksoplysninger(behandlingID)
            .finnSedDokument().orElse(null)

    private fun hentBehandlingMedSaksoplysninger(behandlingID: Long): Behandling = behandlingRepository
        .findWithSaksopplysningerById(behandlingID) ?: throw TekniskException("Fant ikke behandling for $behandlingID")

    private fun lagRapport() {
        val status = migreringsRapport.statusEtterKjøring()
        log.info(status)
        migreringsRapport.saveStatusFiles(status)
    }
}
