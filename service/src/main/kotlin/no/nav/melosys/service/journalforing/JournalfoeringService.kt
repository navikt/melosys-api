package no.nav.melosys.service.journalforing

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


private val log = KotlinLogging.logger { }


@Service
class JournalfoeringService(
    private val journalfoeringValidering: JournalfoeringValidering,
    private val joarkFasade: JoarkFasade,
    private val prosessinstansService: ProsessinstansService,
    private val eessiService: EessiService,
    private val fagsakService: FagsakService,
    private val persondataFasade: PersondataFasade,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val behandlingService: BehandlingService,
    private val utenlandskMyndighetService: UtenlandskMyndighetService,
) {
    fun hentJournalpost(journalpostID: String): Journalpost = joarkFasade.hentJournalpost(journalpostID)

    fun finnHovedpartIdent(journalpost: Journalpost): Optional<String> {
        if (journalpost.brukerIdType == null || journalpost.brukerId == null) {
            return Optional.empty()
        }
        return when (journalpost.brukerIdType!!) {
            BrukerIdType.FOLKEREGISTERIDENT, BrukerIdType.ORGNR -> Optional.of(journalpost.brukerId)
            BrukerIdType.AKTØR_ID -> Optional.of(persondataFasade.hentFolkeregisterident(journalpost.brukerId))
        }
    }

    fun finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID: String): Optional<Behandlingstema> =
        eessiService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID)

    @Transactional
    fun journalførOgOpprettSak(journalfoeringDto: JournalfoeringOpprettDto) {
        val journalpost = hentJournalpost(journalfoeringDto.journalpostID)

        journalfoeringValidering.validerJournalførOgOpprettSak(journalfoeringDto, journalpost)

        val sakstype = Sakstyper.valueOf(journalfoeringDto.fagsak.sakstype)
        val sakstema = Sakstemaer.valueOf(journalfoeringDto.fagsak.sakstema)
        val behandlingstema = Behandlingstema.valueOf(journalfoeringDto.behandlingstemaKode)
        val behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.behandlingstypeKode)

        prosessinstansService.opprettProsessinstansJournalføringNySak(
            journalfoeringDto.tilJournalfoeringOpprettRequest(),
            if (journalfoeringDto.brukerID.isNullOrBlank()) ProsessType.JFR_NY_SAK_VIRKSOMHET else ProsessType.JFR_NY_SAK_BRUKER,
            journalfoeringValidering.skalSetteSøknadslandOgPeriode(sakstype, sakstema, behandlingstema, behandlingstype),
            utledMottaksdato(journalfoeringDto.mottattDato, journalpost),
            UtledBehandlingsaarsak.utledÅrsaktype(journalpost, sakstema, behandlingstema, behandlingstype),
            finnInstitusjonIdEllerNull(journalfoeringDto.avsenderID),
            journalpost.mottaksKanalErElektronisk()
        )
        log.info("Ny sak bestilt etter journalføring av journalpost {}", journalfoeringDto.journalpostID)
    }

    @Transactional
    fun journalførOgKnyttTilEksisterendeSak(journalfoeringDto: JournalfoeringTilordneDto) {
        val journalpost = joarkFasade.hentJournalpost(journalfoeringDto.journalpostID)
        val saksnummer = journalfoeringDto.saksnummer
        val fagsak = fagsakService.hentFagsak(saksnummer)

        journalfoeringValidering.validerJournalførOgKnyttTilEksisterendeSak(journalfoeringDto, journalpost, fagsak)

        log.info("${SubjectHandler.getSaksbehandlerIdent()} knytter journalpost ${journalfoeringDto.journalpostID} til eksisterende sak $saksnummer")
        prosessinstansService.opprettProsessinstansJournalføringKnyttTilEksisterende(
            journalfoeringDto.tilJournalfoeringTilordneRequest(),
            saksnummer,
            fagsak,
            finnInstitusjonIdEllerNull(journalfoeringDto.avsenderID),
            journalpost.mottaksKanalErElektronisk()
        )
    }

    @Transactional
    fun journalførOgOpprettAndregangsBehandling(journalfoeringDto: JournalfoeringTilordneDto) {
        val journalpost = joarkFasade.hentJournalpost(journalfoeringDto.journalpostID)
        val saksnummer = journalfoeringDto.saksnummer
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandlingstema = Behandlingstema.valueOf(journalfoeringDto.behandlingstemaKode)
        val behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.behandlingstypeKode)

        journalfoeringValidering.validerJournalførOgOpprettAndregangsbehandling(journalfoeringDto, journalpost, fagsak, behandlingstema, behandlingstype)

        val sistBehandling = fagsak.hentSistRegistrertBehandling()
        if (sistBehandling.erAktiv() && !sistBehandling.erÅrsavregning()) {
            behandlingService.avsluttBehandling(sistBehandling.id)
        }

        log.info("${SubjectHandler.getSaksbehandlerIdent()} knytter journalpost ${journalfoeringDto.journalpostID}} til sak $saksnummer og lager ny vurdering")
        prosessinstansService.journalførOgOpprettAndregangsBehandling(
            finnProsessTypeForAndregangsbehandling(behandlingstype, behandlingstema, fagsak),
            behandlingstema,
            behandlingstype,
            journalfoeringDto.tilJournalfoeringTilordneRequest(),
            UtledBehandlingsaarsak.utledÅrsaktype(journalpost, fagsak.tema, behandlingstema, behandlingstype),
            utledMottaksdato(journalfoeringDto.mottattDato, journalpost),
            finnInstitusjonIdEllerNull(journalfoeringDto.avsenderID),
            journalpost.mottaksKanalErElektronisk()
        )
    }

    @Transactional
    fun journalførSed(journalfoeringSedDto: JournalfoeringSedDto) {
        journalfoeringValidering.validerJournalførSed(journalfoeringSedDto)

        val eessiMelding = eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.journalpostID)
        val aktørID = persondataFasade.hentAktørIdForIdent(journalfoeringSedDto.brukerID)

        prosessinstansService.opprettProsessinstansSedMottak(eessiMelding, aktørID)
    }


    private fun finnInstitusjonIdEllerNull(avsenderID: String?): String? =
        utenlandskMyndighetService.finnInstitusjonID(avsenderID).orElse(null)

    private fun finnProsessTypeForAndregangsbehandling(
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        fagsak: Fagsak
    ): ProsessType =
        if (saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, behandlingstype, behandlingstema)) {
            ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING
        } else ProsessType.JFR_ANDREGANG_NY_BEHANDLING

    companion object {
        private fun utledMottaksdato(datoFraSaksbehandler: LocalDate?, journalpost: Journalpost): LocalDate =
            datoFraSaksbehandler ?: LocalDate.ofInstant(journalpost.forsendelseMottatt, ZoneId.systemDefault())
    }
}
