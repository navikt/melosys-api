package no.nav.melosys.service.oppgave

import jakarta.annotation.Nullable
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.util.MottatteOpplysningerUtils
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveFactory.Companion.lagJournalføringsoppgave
import no.nav.melosys.service.oppgave.dto.*
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class OppgaveService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val oppgaveFasade: OppgaveFasade,
    private val saksopplysningerService: SaksopplysningerService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val persondataFasade: PersondataFasade,
    private val eregFasade: EregFasade,
    private val utledMottaksdato: UtledMottaksdato,
    private val oppgaveFactory: OppgaveFactory
) {
    fun hentOppgaverMedAnsvarlig(ansvarligID: String): List<OppgaveDto?> =
        oppgaveFasade.finnOppgaverMedAnsvarlig(ansvarligID).tilDtoer()

    fun ferdigstillOppgave(oppgaveID: String) {
        log.info("Ferdigstiller oppgave {}", oppgaveID)
        oppgaveFasade.ferdigstillOppgave(oppgaveID)
    }

    fun ferdigstillOppgaveMedSaksnummer(fagsaksnummer: String) =
        finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsaksnummer).ifPresentOrElse(
            { oppgave: Oppgave -> ferdigstillOppgave(oppgave.oppgaveId) }
        ) { log.warn("Sak $fagsaksnummer har ingen oppgaver å ferdigstille.") }

    fun leggTilbakeBehandlingsoppgaveMedSaksnummer(fagSaksnummer: String) {
        val oppgave = hentÅpenBehandlingsoppgaveMedFagsaksnummer(fagSaksnummer)
        oppgaveFasade.leggTilbakeOppgave(oppgave.oppgaveId)
    }

    fun finnSisteAvsluttetBehandlingsoppgaveMedFagsaksnummer(saksnummer: String): Optional<Oppgave> =
        oppgaveFasade.finnAvsluttetBehandlingsoppgaverMedSaksnummer(saksnummer).stream()
            .filter { oppgave: Oppgave -> filtrerUtAvgiftsoppgaver(oppgave) }.max(Comparator.comparing { obj: Oppgave -> obj.opprettetTidspunkt })

    fun finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer: String): Optional<Oppgave> {
        val oppgaver = oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer).stream()
            .filter { oppgave: Oppgave -> filtrerUtAvgiftsoppgaver(oppgave) }.toList()
        return if (oppgaver.isNotEmpty()) {
            if (oppgaver.size > 1) {
                throw TekniskException("Det finnes flere aktive behandlingsoppgaver for sak $saksnummer")
            }
            Optional.of(oppgaver[0])
        } else {
            Optional.empty()
        }
    }

    private fun filtrerUtAvgiftsoppgaver(oppgave: Oppgave): Boolean = !(Tema.TRY == oppgave.tema && Oppgavetyper.VUR == oppgave.oppgavetype)

    fun hentÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer: String): Oppgave =
        finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer).orElseThrow { IkkeFunnetException("Finner ingen åpen oppgave med saksnummer $saksnummer") }

    fun hentOppgaveMedOppgaveID(oppgaveID: String): Oppgave = oppgaveFasade.hentOppgave(oppgaveID)

    fun hentSistAktiveBehandling(saksnummer: String): Behandling = fagsakService.hentFagsak(saksnummer).hentSistAktivBehandling()

    @JvmOverloads
    fun opprettEllerGjenbrukBehandlingsoppgave(
        behandling: Behandling,
        @Nullable journalpostID: String?,
        @Nullable aktørID: String?,
        @Nullable tilordnetRessurs: String?,
        @Nullable orgnr: String? = null
    ) {
        val eksisterendeOppgave = finnÅpenBehandlingsoppgaveMedFagsaksnummer(behandling.fagsak.saksnummer)
        if (eksisterendeOppgave.isEmpty) {
            val oppgaveBuilder =
                lagBehandlingsoppgave(behandling).setTilordnetRessurs(tilordnetRessurs).setJournalpostId(journalpostID).setAktørId(aktørID)
                    .setOrgnr(orgnr).setSaksnummer(behandling.fagsak.saksnummer)
            val oppgaveID = if (StringUtils.isNotEmpty(aktørID) && harBeskyttelsesbehov(behandling.id)) oppgaveFasade.opprettSensitivOppgave(
                oppgaveBuilder.build()
            ) else oppgaveFasade.opprettOppgave(oppgaveBuilder.build())
            log.info("Opprettet oppgave $oppgaveID for behandling ${behandling.id}")
        } else if (tilordnetRessurs != null && tilordnetRessurs != eksisterendeOppgave.get().tilordnetRessurs) {
            log.info("Oppgave eksisterer, oppdaterer tilordnetRessurs for oppgave tilknyttet behandling ${behandling.id}")
            tildelOppgave(eksisterendeOppgave.get().oppgaveId, tilordnetRessurs)
        } else {
            log.info("Oppgave tilknyttet behandling ${behandling.id} eksisterer og er allerede tilordnet ressurs $tilordnetRessurs.")
        }
    }

    fun lagBehandlingsoppgave(behandling: Behandling): Oppgave.Builder =
        oppgaveFactory.lagBehandlingsoppgave(behandling, utledMottaksdato.getMottaksdato(behandling))
        { behandlingService.hentBehandlingMedSaksopplysninger(behandling.id).finnSedDokument().orElse(null) }

    fun opprettJournalføringsoppgave(journalpostID: String, aktørID: String?) {
        val oppgaveID = opprettOppgave(lagJournalføringsoppgave(journalpostID).setAktørId(aktørID).build())
        log.info("Journalføringsoppgave {} opprettet for journalpost {}", oppgaveID, journalpostID)
    }

    fun opprettOppgave(oppgave: Oppgave): String {
        log.info("Starter med å opprette oppgave med journalpostId {}", oppgave.journalpostId)
        return oppgaveFasade.opprettOppgave(oppgave)
    }

    fun oppdaterOppgave(oppgaveID: String, oppgaveOppdatering: OppgaveOppdatering?) = oppgaveFasade.oppdaterOppgave(oppgaveID, oppgaveOppdatering)

    fun oppdaterOppgave(oppgaveID: String, behandling: Behandling) {
        val behandlingsoppgave = lagBehandlingsoppgave(behandling).build()
        oppdaterOppgave(
            oppgaveID,
            OppgaveOppdatering.builder().behandlingstema(behandlingsoppgave.behandlingstema).tema(behandlingsoppgave.tema)
                .oppgavetype(behandlingsoppgave.oppgavetype).fristFerdigstillelse(
                    Behandling.utledBehandlingsfrist(
                        behandling, utledMottaksdato.getMottaksdato(behandling)
                    )
                ).beskrivelse(behandlingsoppgave.beskrivelse).build()
        )
    }

    fun oppdaterOppgaveMedSaksnummer(fagSaksnummer: String, oppgaveOppdatering: OppgaveOppdatering) =
        finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagSaksnummer).ifPresentOrElse({ oppg: Oppgave ->
            oppdaterOppgave(
                oppg.oppgaveId,
                oppgaveOppdatering
            )
        }) {
            log.warn(
                "Sak {} har ingen åpne oppgaver å oppdatere.",
                fagSaksnummer
            )
        }

    fun tildelOppgave(oppgaveID: String, saksbehandler: String) =
        oppgaveFasade.oppdaterOppgave(oppgaveID, OppgaveOppdatering.builder().tilordnetRessurs(saksbehandler).build())

    fun opprettOppgaveForSak(saksnummer: String) {
        log.info("Oppretter ny oppgave for saksnummer {}", saksnummer)
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentSistAktivBehandling()
        val oppgave = finnSisteAvsluttetBehandlingsoppgaveMedFagsaksnummer(saksnummer)
        val tilordnetRessurs = oppgave.map { obj: Oppgave -> obj.tilordnetRessurs }.orElse(null)
        opprettEllerGjenbrukBehandlingsoppgave(
            behandling, behandling.initierendeJournalpostId, fagsak.hentBrukersAktørID(), tilordnetRessurs, null
        )
    }

    fun saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler: String, saksnummer: String): Boolean =
        finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
            .map { obj: Oppgave -> obj.tilordnetRessurs }
            .filter { anObject: String? -> saksbehandler.equals(anObject) }
            .isPresent

    private fun oppgaverTilDtoer(oppgaverFraDomain: Collection<Oppgave>): List<OppgaveDto> =
        oppgaverFraDomain.map { tilOppgaveDtoHåndterException(it) }.filterNotNull()

    private fun Collection<Oppgave>.tilDtoer(): List<OppgaveDto> = this.map { tilOppgaveDtoHåndterException(it) }.filterNotNull()

    @Nullable
    private fun tilOppgaveDtoHåndterException(oppgave: Oppgave): OppgaveDto? =
        try {
            tilOppgaveDto(oppgave)
        } catch (e: Exception) {
            log.error("Kan ikke mappe oppgave {}", oppgave.oppgaveId, e)
            null
        }

    private fun tilOppgaveDto(oppgave: Oppgave): OppgaveDto {
        val resultat: OppgaveDto
        resultat = if (oppgave.erJournalFøring()) {
            lagJournalføringsoppgaveDto(oppgave)
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling() || oppgave.erVurderHenvendelse() || oppgave.erManglendeInnbetalingBehandling()) {
            lagBehandlingsoppgaveDto(oppgave)
        } else {
            throw TekniskException("Oppgavetype " + oppgave.oppgavetype + " støttes ikke")
        }
        resultat.aktivTil = oppgave.fristFerdigstillelse
        resultat.ansvarligID = oppgave.tilordnetRessurs
        resultat.oppgaveID = oppgave.oppgaveId
        resultat.prioritet = oppgave.prioritet
        resultat.versjon = oppgave.versjon
        return resultat
    }

    private fun lagJournalføringsoppgaveDto(oppgave: Oppgave): OppgaveDto {
        val journalfoeringsoppgaveDto = JournalfoeringsoppgaveDto()
        journalfoeringsoppgaveDto.journalpostID = oppgave.journalpostId
        val aktørId = oppgave.aktørId
        val orgnr = oppgave.orgnr
        oppdaterHovedpartIdentOgNavn(aktørId, orgnr, journalfoeringsoppgaveDto)
        return journalfoeringsoppgaveDto
    }

    private fun oppdaterHovedpartIdentOgNavn(aktørID: String?, orgnr: String?, oppgaveDto: OppgaveDto) {
        if (aktørID != null) {
            val fnr = persondataFasade.finnFolkeregisterident(aktørID).orElse(null)
            if (StringUtils.isNotEmpty(fnr)) {
                oppgaveDto.hovedpartIdent = fnr
                oppgaveDto.navn = persondataFasade.hentSammensattNavn(fnr)
                return
            }
        }
        if (orgnr != null) {
            oppgaveDto.hovedpartIdent = orgnr
            oppgaveDto.navn = eregFasade.hentOrganisasjonNavn(orgnr)
            return
        }
        oppgaveDto.hovedpartIdent = UKJENT
        oppgaveDto.navn = UKJENT
    }

    private fun lagBehandlingsoppgaveDto(oppgave: Oppgave): OppgaveDto {
        val fagsak = fagsakService.hentFagsak(oppgave.saksnummer)

        var behandling = fagsak.hentSistAktivBehandling()
        behandling = behandlingService.hentBehandling(behandling.id)

        val behOppgaveDto = BehandlingsoppgaveDto().apply {
            oppgaveBeskrivelse = oppgave.beskrivelse
            saksnummer = fagsak.saksnummer
            sakstype = fagsak.type
            sakstema = fagsak.tema
        }
        behOppgaveDto.behandling = mapBehandling(behandling)
        behOppgaveDto.sisteNotat = hentSisteBehandlingsNotat(behandling)

        val aktørID = fagsak.finnBrukersAktørID().orElse(null)
        val orgnr = fagsak.finnVirksomhetsOrgnr().orElse(null)
        oppdaterHovedpartIdentOgNavn(aktørID, orgnr, behOppgaveDto)
        if (orgnr != null) {
            return behOppgaveDto
        }

        val sedopplysninger = saksopplysningerService.finnSedOpplysninger(behandling.id)
        if (sedopplysninger.isPresent) {
            val sedDokument = sedopplysninger.get()
            behOppgaveDto.land = SoeknadslandDto.av(sedDokument.lovvalgslandKode)
            behOppgaveDto.periode = PeriodeDto(
                sedDokument.lovvalgsperiode.fom, sedDokument.lovvalgsperiode.tom
            )
            return behOppgaveDto
        }

        mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).ifPresent {
            val mottatteOpplysningerData = it.mottatteOpplysningerData
            val søknadsland = MottatteOpplysningerUtils.hentLand(mottatteOpplysningerData)
            behOppgaveDto.land = SoeknadslandDto.av(søknadsland)
            behOppgaveDto.periode = mapPeriode(mottatteOpplysningerData)
        }
        return behOppgaveDto
    }

    private fun hentSisteBehandlingsNotat(behandling: Behandling): String? =
        if (behandling.behandlingsnotater.isEmpty()) {
            null
        } else {
            Collections.max(behandling.behandlingsnotater, Comparator.comparing { obj: Behandlingsnotat -> obj.registrertDato }).tekst
        }

    private fun mapBehandling(behandling: Behandling): BehandlingDto =
        BehandlingDto().apply {
            behandlingID = behandling.id
            behandlingsstatus = behandling.status
            behandlingstype = behandling.type
            behandlingstema = behandling.tema
            registrertDato = behandling.registrertDato
            endretDato = behandling.endretDato
            svarFrist = behandling.dokumentasjonSvarfristDato
            // FIXME: Feltet og endepunktet fjernes fra JSON-schema
            isErUnderOppdatering = false
        }

    private fun harBeskyttelsesbehov(behandlingID: Long): Boolean {
        val brukersAktørID = getBrukersAktørID(behandlingID)
        if (persondataFasade.harStrengtFortroligAdresse(brukersAktørID)) {
            return true
        }
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
        if (behandling.mottatteOpplysninger == null) {
            return false
        }
        for (fnr in behandling.mottatteOpplysninger.mottatteOpplysningerData.hentFnrMedfølgendeBarn()) {
            if (persondataFasade.harStrengtFortroligAdresse(fnr)) {
                return true
            }
        }
        return false
    }

    private fun getBrukersAktørID(behandlingID: Long): String {
        // Om behandlingService.hentBehandlingMedSaksopplysninger benyttes behandling ikke har saksopplysninger
        // feiler getFagsak().hentBrukersAktørID() med LazyInitializationException: could not initialize proxy
        // https://jira.adeo.no/browse/MELOSYS-5871
        val behandling = behandlingService.hentBehandling(behandlingID)
        return behandling.fagsak.hentBrukersAktørID()
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppgaveService::class.java)
        private const val UKJENT = "UKJENT"
        private fun mapPeriode(mottatteOpplysningerData: MottatteOpplysningerData): PeriodeDto {
            val periode = MottatteOpplysningerUtils.hentPeriode(mottatteOpplysningerData) ?: return PeriodeDto(null, null)
            return PeriodeDto(periode.getFom(), periode.getTom())
        }
    }
}
