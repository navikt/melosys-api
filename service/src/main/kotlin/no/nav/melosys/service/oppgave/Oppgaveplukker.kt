package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveFactory.Companion.erGyldigOppgave
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Service
class Oppgaveplukker(
    private val oppgaveFasade: OppgaveFasade,
    private val oppgaveTilbakkeleggingRepo: OppgaveTilbakeleggingRepository,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val oppgaveFactory: OppgaveFactory
) {
    private val log = KotlinLogging.logger { }

    @Transactional
    @Synchronized
    fun plukkOppgave(saksbehandlerID: String, plukkDto: PlukkOppgaveInnDto): Oppgave? {
        log.info("Begynner plukking av oppgave for saksbehandler med følgende kriterier: $plukkDto")

        val utildelteOppgaver = hentUtildelteOppgaver(plukkDto)
        log.info("Funnet ${utildelteOppgaver.size} oppgaver ")

        val valgtOppgave = filtrerOppgaver(plukkDto, utildelteOppgaver).maxWithOrNull(Oppgave.LAVEST_TIL_HØYEST_PRIORITET)
        if (valgtOppgave == null) {
            log.info("Ingen oppgave kunne plukkes med følgende kriterier $plukkDto")
        } else {
            oppdaterBehandlingsstatus(valgtOppgave.saksnummer)
            oppgaveService.tildelOppgave(valgtOppgave.oppgaveId, saksbehandlerID)
            log.info("Oppgave ${valgtOppgave.oppgaveId} ble plukket.")
        }
        return valgtOppgave
    }

    fun hentUtildelteOppgaver(plukkDto: PlukkOppgaveInnDto): List<Oppgave> {
        val oppgaveBehandlingstemaSet =
            hentAlleOppgaveBehandlingstemaTilSøk(plukkDto.sakstype, plukkDto.sakstema, plukkDto.behandlingstema)
        return oppgaveBehandlingstemaSet.flatMap {
            oppgaveFasade.finnUtildelteOppgaverEtterFrist(it)
        }
    }

    private fun filtrerOppgaver(plukkDto: PlukkOppgaveInnDto, utildelteOppgaver: List<Oppgave>): List<Oppgave> {
        val saksnumre = utildelteOppgaver.map { obj: Oppgave -> obj.saksnummer }
        val sasksnummerFagsakMap = fagsakService.hentFagsaker(saksnumre).associateBy { it.saksnummer }
        var antallSakSomIkkeMatcherSøk = 0
        var antallSakSomVenter = 0
        val filtrerteOppgaver = ArrayList<Oppgave>()

        utildelteOppgaver.forEach {
            val fagsak = sasksnummerFagsakMap[it.saksnummer]
            if (fagsak == null) {
                log.warn("Fant ikke fagsak ${it.saksnummer} for oppgave ${it.oppgaveId}")
                return@forEach
            }

            val fagsakMatcherSøk = fagsakMatcherSøk(fagsak, plukkDto)
            val venterPåDokEllerAvklaring = venterPåDokumentasjonEllerFagligAvklaring(fagsak)
            if (!fagsakMatcherSøk) antallSakSomIkkeMatcherSøk++
            if (venterPåDokEllerAvklaring) antallSakSomVenter++
            if (fagsakMatcherSøk && !venterPåDokEllerAvklaring) filtrerteOppgaver.add(it)
        }
        if (antallSakSomIkkeMatcherSøk > 0) {
            log.info("Antall sak som ikke matcher søk: $antallSakSomIkkeMatcherSøk / ${saksnumre.size}")
        }
        log.info("Antall sak som venter på dokumentasjon eller avklaring: $antallSakSomVenter / ${saksnumre.size}")
        return filtrerteOppgaver
    }

    private fun fagsakMatcherSøk(fagsak: Fagsak, plukkDto: PlukkOppgaveInnDto): Boolean =
        fagsak.type == plukkDto.sakstype
            && fagsak.tema == plukkDto.sakstema
            && fagsak.behandlinger.any { it.tema == plukkDto.behandlingstema }

    private fun venterPåDokumentasjonEllerFagligAvklaring(fagsak: Fagsak): Boolean {
        val behandling = fagsak.hentSistAktivBehandling()
        if (behandling.erVenterForDokumentasjon()) {
            if (behandling.dokumentasjonSvarfristDato == null) {
                log.warn(
                    "Behandling ${behandling.id} tilhørende ${fagsak.saksnummer} avventer dokumentasjon, men har ingen svarfristdato."
                )
                return true
            }
            return Instant.now().isBefore(behandling.dokumentasjonSvarfristDato)
        }
        return behandling.harStatus(Behandlingsstatus.AVVENT_FAGLIG_AVKLARING)
    }

    private fun oppdaterBehandlingsstatus(saksnummer: String) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.finnAktivBehandling()
        if (behandling != null && (behandling.status == Behandlingsstatus.SVAR_ANMODNING_MOTTATT || behandling.status == Behandlingsstatus.OPPRETTET)) {
            behandling.status = Behandlingsstatus.UNDER_BEHANDLING
            behandlingService.lagre(behandling)
        }
    }

    private fun hentAlleOppgaveBehandlingstemaTilSøk(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema
    ): Set<String> =
        Behandlingstyper.values()
            .filter {
                erGyldigOppgave(
                    sakstype,
                    sakstema,
                    behandlingstema,
                    it
                )
            }.mapNotNull {
                oppgaveFactory.utledOppgaveBehandlingstema(
                    sakstype,
                    sakstema,
                    behandlingstema,
                    it
                )
            }.map { it.kode }.toSet()

    @Transactional
    @Synchronized
    fun leggTilbakeOppgave(saksbehandlerID: String, tilbakelegging: TilbakeleggingDto) {
        val behandling = behandlingService.hentBehandling(tilbakelegging.behandlingID)
        val fagsak = behandling.fagsak
        val oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        val oppgaveID = oppgave.oppgaveId
        if (!tilbakelegging.isVenterPåDokumentasjon) {
            oppgaveTilbakkeleggingRepo.save(
                OppgaveTilbakelegging().apply {
                    oppgaveId = oppgaveID
                    saksbehandlerId = saksbehandlerID
                    registrertDato = LocalDateTime.now()
                }
            )
        }
        oppgaveFasade.leggTilbakeOppgave(oppgaveID)
        log.info("Oppgave med oppgaveId $oppgaveID er lagt tilbake. ")
    }
}
