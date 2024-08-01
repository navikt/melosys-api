package no.nav.melosys.service.oppgave

import jakarta.annotation.Nullable
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.jvm.optionals.getOrNull

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
    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun hentOppgaverMedAnsvarlig(ansvarligID: String): List<OppgaveDto> =
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

    fun finnSisteAvsluttetBehandlingsoppgaveMedFagsaksnummer(saksnummer: String): Oppgave? =
        oppgaveFasade.finnAvsluttetBehandlingsoppgaverMedSaksnummer(saksnummer)
            .filter { filtrerUtAvgiftsoppgaver(it) }
            .maxByOrNull { it.opprettetTidspunkt }

    fun finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer: String): Optional<Oppgave> {
        val oppgaver = oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)
            .filter { filtrerUtAvgiftsoppgaver(it) }
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

    fun hentSistAktiveBehandling(saksnummer: String): Behandling = fagsakService.hentFagsak(saksnummer).hentSistAktivBehandlingIkkeÅrsavregning()

    @JvmOverloads
    fun opprettEllerGjenbrukBehandlingsoppgave(
        behandling: Behandling,
        @Nullable journalpostID: String?,
        @Nullable aktørID: String?,
        @Nullable tilordnetRessurs: String?,
        @Nullable orgnr: String? = null
    ) {
        val eksisterendeOppgave =
            if (behandling.oppgaveId != null) oppgaveFasade.hentOppgave(behandling.oppgaveId) else
                finnÅpenBehandlingsoppgaveMedFagsaksnummer(behandling.fagsak.saksnummer).getOrNull()
        if (eksisterendeOppgave == null) {
            val oppgaveBuilder =
                lagBehandlingsoppgave(behandling).setTilordnetRessurs(tilordnetRessurs).setJournalpostId(journalpostID).setAktørId(aktørID)
                    .setOrgnr(orgnr).setSaksnummer(behandling.fagsak.saksnummer)
            val oppgaveID =
                if (StringUtils.isNotEmpty(aktørID) && harBeskyttelsesbehov(behandling.id))
                    oppgaveFasade.opprettSensitivOppgave(oppgaveBuilder.build())
                else oppgaveFasade.opprettOppgave(oppgaveBuilder.build())
            settOppgaveIdPåBehandling(behandling, oppgaveID)
            log.info("Opprettet oppgave $oppgaveID for behandling ${behandling.id}")
        } else if (tilordnetRessurs != eksisterendeOppgave.tilordnetRessurs) {
            log.info("Oppgave eksisterer, oppdaterer tilordnetRessurs for oppgave tilknyttet behandling ${behandling.id}")
            tildelOppgave(eksisterendeOppgave.oppgaveId, tilordnetRessurs)
        } else {
            log.info("Oppgave tilknyttet behandling ${behandling.id} eksisterer og er allerede tilordnet ressurs $tilordnetRessurs.")
        }

        // TODO: Denne blir ikke nødvendig etter migreringen er ferdig. MELOSYS-6707
        if (behandling.oppgaveId == null && eksisterendeOppgave != null) {
            settOppgaveIdPåBehandling(behandling, eksisterendeOppgave.oppgaveId)
        }
    }

    fun settOppgaveIdPåBehandling(behandling: Behandling, oppgaveId: String) {
        behandling.oppgaveId = oppgaveId
        behandlingService.lagre(behandling)
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

    fun tildelOppgave(oppgaveID: String, saksbehandler: String?) =
        oppgaveFasade.oppdaterOppgave(oppgaveID, OppgaveOppdatering.builder().tilordnetRessurs(saksbehandler).build())

    fun opprettOppgaveForSak(saksnummer: String) {
        log.info("Oppretter ny oppgave for saksnummer {}", saksnummer)
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        val oppgave = finnSisteAvsluttetBehandlingsoppgaveMedFagsaksnummer(saksnummer)
        val tilordnetRessurs = oppgave?.tilordnetRessurs
        opprettEllerGjenbrukBehandlingsoppgave(
            behandling, behandling.initierendeJournalpostId, fagsak.hentBrukersAktørID(), tilordnetRessurs, null
        )
    }

    fun saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler: String, saksnummer: String): Boolean =
        finnÅpenBehandlingsoppgaveMedFagsaksnummer(saksnummer)
            .map { obj: Oppgave -> obj.tilordnetRessurs }
            .filter { anObject: String? -> saksbehandler.equals(anObject) }
            .isPresent

    private fun Collection<Oppgave>.tilDtoer(): List<OppgaveDto> = this.mapNotNull { tilOppgaveDtoHåndterException(it) }

    @Nullable
    private fun tilOppgaveDtoHåndterException(oppgave: Oppgave): OppgaveDto? =
        try {
            tilOppgaveDto(oppgave)
        } catch (e: Exception) {
            log.warn("Kan ikke mappe oppgave ${oppgave.oppgaveId}", e)
            null
        }

    private fun tilOppgaveDto(oppgave: Oppgave): OppgaveDto =
        if (oppgave.erJournalFøring()) {
            lagJournalføringsoppgaveDto(oppgave)
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling() || oppgave.erVurderHenvendelse() || oppgave.erManglendeInnbetalingBehandling()) {
            lagBehandlingsoppgaveDto(oppgave)
        } else {
            throw TekniskException("Oppgavetype ${oppgave.oppgavetype} støttes ikke")
        }

    private fun lagJournalføringsoppgaveDto(oppgave: Oppgave): JournalfoeringsoppgaveDto = JournalfoeringsoppgaveDto(
        aktivTil = oppgave.fristFerdigstillelse,
        ansvarligID = oppgave.tilordnetRessurs,
        oppgaveID = oppgave.oppgaveId,
        prioritet = oppgave.prioritet,
        navn = hentNavn(oppgave),
        hovedpartIdent = hentHovedpartIdent(oppgave),
        versjon = oppgave.versjon,
        journalpostID = oppgave.journalpostId
    )

    private fun lagBehandlingsoppgaveDto(oppgave: Oppgave): BehandlingsoppgaveDto {
        val fagsak = fagsakService.hentFagsak(oppgave.saksnummer)
        val tilknyttetBehandling = fagsak.behandlinger.firstOrNull { it.oppgaveId == oppgave.oppgaveId }
            ?: throw TekniskException("Fant ikke behandling til oppgave ${oppgave.oppgaveId}")
        val orgnr = fagsak.finnVirksomhetsOrgnr()

        return BehandlingsoppgaveDto(
            aktivTil = oppgave.fristFerdigstillelse,
            ansvarligID = oppgave.tilordnetRessurs,
            oppgaveID = oppgave.oppgaveId,
            prioritet = oppgave.prioritet,
            navn = hentNavn(oppgave),
            hovedpartIdent = hentHovedpartIdent(oppgave),
            versjon = oppgave.versjon,
            behandling = mapBehandling(tilknyttetBehandling),
            saksnummer = fagsak.saksnummer,
            sakstype = fagsak.type,
            sakstema = fagsak.tema,
            land = hentLand(orgnr, tilknyttetBehandling.id),
            periode = hentPeriode(orgnr, tilknyttetBehandling.id),
            oppgaveBeskrivelse = oppgave.beskrivelse,
            sisteNotat = hentSisteBehandlingsNotat(tilknyttetBehandling),
        )
    }

    private fun hentSisteBehandlingsNotat(behandling: Behandling): String? =
        behandling.behandlingsnotater.maxByOrNull { it.registrertDato }?.tekst

    private fun hentNavn(oppgave: Oppgave): String {
        if (oppgave.aktørId != null) {
            val fnr = persondataFasade.finnFolkeregisterident(oppgave.aktørId).orElse(null)
            if (StringUtils.isNotEmpty(fnr)) {
                return persondataFasade.hentSammensattNavn(fnr)
            }
        }
        if (oppgave.orgnr != null) {
            return eregFasade.hentOrganisasjonNavn(oppgave.orgnr)
        }
        return UKJENT
    }

    private fun hentHovedpartIdent(oppgave: Oppgave): String {
        if (oppgave.aktørId != null) {
            val fnr = persondataFasade.finnFolkeregisterident(oppgave.aktørId).orElse(null)
            if (StringUtils.isNotEmpty(fnr)) {
                return fnr
            }
        }
        if (oppgave.orgnr != null) {
            return oppgave.orgnr
        }
        return UKJENT
    }

    private fun hentLand(orgnr: String?, sistAktivBehandlingID: Long): SoeknadslandDto? {
        if (orgnr != null) return null

        val sedopplysninger = saksopplysningerService.finnSedOpplysninger(sistAktivBehandlingID)
        if (sedopplysninger.isPresent)
            return SoeknadslandDto.av(sedopplysninger.get().lovvalgslandKode)

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(sistAktivBehandlingID)
        if (mottatteOpplysninger.isPresent)
            return SoeknadslandDto.av(MottatteOpplysningerUtils.hentLand(mottatteOpplysninger.get().mottatteOpplysningerData))

        return null
    }

    private fun hentPeriode(orgnr: String?, sistAktivBehandlingID: Long): PeriodeDto? {
        if (orgnr != null) return null

        val sedopplysninger = saksopplysningerService.finnSedOpplysninger(sistAktivBehandlingID)
        if (sedopplysninger.isPresent)
            return PeriodeDto(sedopplysninger.get().lovvalgsperiode.fom, sedopplysninger.get().lovvalgsperiode.tom)

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(sistAktivBehandlingID)
        if (mottatteOpplysninger.isPresent)
            return mapPeriode(mottatteOpplysninger.get().mottatteOpplysningerData)

        return null
    }

    private fun mapBehandling(behandling: Behandling): BehandlingDto = BehandlingDto(
        behandling.id,
        behandling.type,
        behandling.tema,
        behandling.status,
        // FIXME: Feltet og endepunktet fjernes fra JSON-schema
        false,
        behandling.registrertDato,
        behandling.endretDato,
        behandling.dokumentasjonSvarfristDato
    )

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
        private const val UKJENT = "UKJENT"
        private fun mapPeriode(mottatteOpplysningerData: MottatteOpplysningerData): PeriodeDto {
            val periode = MottatteOpplysningerUtils.hentPeriode(mottatteOpplysningerData) ?: return PeriodeDto(
                null,
                null
            )
            return PeriodeDto(periode.getFom(), periode.getTom())
        }
    }
}
