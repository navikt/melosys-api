package no.nav.melosys.service.journalforing

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Component

@Component
class JournalfoeringValidering(
    private val lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService,
    private val eessiService: EessiService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val fagsakService: FagsakService,
) {

    internal fun validerJournalførOgOpprettSak(journalfoeringDto: JournalfoeringOpprettDto, journalpost: Journalpost) {
        validerJournalpostIkkeAlleredeFerdigstilt(journalpost)

        val sakstype = Sakstyper.valueOf(journalfoeringDto.fagsak.sakstype)
        val sakstema = Sakstemaer.valueOf(journalfoeringDto.fagsak.sakstema)
        val behandlingstema = Behandlingstema.valueOf(journalfoeringDto.behandlingstemaKode)
        val behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.behandlingstypeKode)
        val hovedpart = journalføringGjelder(journalfoeringDto)

        if (journalfoeringDto.skalSendeForvaltningsmelding()) {
            validerKanSendeForvaltningsmelding(sakstema, behandlingstype, hovedpart)
        }

        if (journalpost.mottaksKanalErEessi()) {
            val melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId)
            validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding)
            validerSedIkkeAlleredeTilknyttetAnnenSak(melosysEessiMelding, null)
        }

        if (journalfoeringDto.fagsak == null) {
            throw FunksjonellException("Opplysninger for å opprette en søknad mangler")
        }

        fellesValidering(journalfoeringDto, journalpost.mottaksKanalErEessi())

        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            hovedpart, sakstype, sakstema, behandlingstema, behandlingstype
        )

        if (journalfoeringDto.avsenderType == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
            validerSakstypeForUtenlandskTrygdemyndighetsland(sakstype, journalfoeringDto.avsenderID)
        }

        if (skalSetteSøknadslandOgPeriode(sakstype, sakstema, behandlingstema, behandlingstype)) {
            validerSøknadsperiodeOgLand(journalfoeringDto)
        }
    }

    internal fun validerJournalførOgKnyttTilEksisterendeSak(journalfoeringDto: JournalfoeringTilordneDto, journalpost: Journalpost, fagsak: Fagsak) {
        validerJournalpostIkkeAlleredeFerdigstilt(journalpost)

        if (journalpost.mottaksKanalErEessi()) {
            val melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId)
            validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding)
            validerSedIkkeAlleredeTilknyttetAnnenSak(melosysEessiMelding, fagsak.saksnummer)
        }

        if (journalfoeringDto.skalSendeForvaltningsmelding()) {
            validerKanSendeForvaltningsmelding(
                fagsak.tema,
                Behandlingstyper.valueOf(journalfoeringDto.behandlingstypeKode),
                journalføringGjelder(journalfoeringDto)
            )
        }

        fellesValidering(journalfoeringDto, journalpost.mottaksKanalErEessi())
    }

    internal fun validerJournalførOgOpprettAndregangsbehandling(
        journalfoeringDto: JournalfoeringTilordneDto,
        journalpost: Journalpost,
        fagsak: Fagsak,
        nyttBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper
    ) {
        validerJournalpostIkkeAlleredeFerdigstilt(journalpost)

        val sistBehandling = fagsak.hentSistRegistrertBehandling()
        val muligeBehandlingstyper =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(
                Aktoersroller.BRUKER,
                fagsak.saksnummer,
                nyttBehandlingstema
            )

        if (!muligeBehandlingstyper.contains(nyBehandlingstype)) {
            throw FunksjonellException("Behandlingstype $nyBehandlingstype er ikke tillatt for behandlingstema $nyttBehandlingstema og fagsak ${fagsak.saksnummer}")
        }

        if (journalpost.mottaksKanalErEessi()) {
            val melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId)
            validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding)
            validerSedIkkeAlleredeTilknyttetAnnenSak(melosysEessiMelding, fagsak.saksnummer)
        }

        val behandlingstema = Behandlingstema.valueOf(journalfoeringDto.behandlingstemaKode)
        val behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.behandlingstypeKode)

        if (journalfoeringDto.skalSendeForvaltningsmelding()) {
            validerKanSendeForvaltningsmelding(fagsak.tema, behandlingstype, journalføringGjelder(journalfoeringDto))
        }

        fellesValidering(journalfoeringDto, journalpost.mottaksKanalErEessi())

        lovligeKombinasjonerSaksbehandlingService.validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(
            fagsak,
            sistBehandling,
            behandlingstema,
            behandlingstype
        )
    }

    internal fun validerJournalførSed(journalfoeringSedDto: JournalfoeringSedDto) {
        when {
            journalfoeringSedDto.journalpostID.isNullOrEmpty() ->
                throw FunksjonellException("JournalpostID er påkrevd!")

            journalfoeringSedDto.brukerID.isNullOrEmpty() ->
                throw FunksjonellException("BrukerID er påkrevd!")

            journalfoeringSedDto.oppgaveID.isNullOrEmpty() ->
                throw FunksjonellException("OppgaveID er påkrevd!")

            !eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.journalpostID) ->
                throw FunksjonellException("Sed tilknyttet journalpost ${journalfoeringSedDto.journalpostID} støtter ikke automatisk behandling!")
        }
    }

    private fun validerJournalpostIkkeAlleredeFerdigstilt(journalpost: Journalpost) {
        if (journalpost.isErFerdigstilt) {
            throw FunksjonellException("Journalposten er allerede ferdigstilt!")
        }
    }

    private fun validerKanSendeForvaltningsmelding(sakstema: Sakstemaer, behandlingstype: Behandlingstyper, hovedpart: Aktoersroller) {
        if (!(sakstema in listOf(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstemaer.TRYGDEAVGIFT) && behandlingstype in listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING))) {
            throw FunksjonellException("Kan kun sende forvaltningsmelding for behandlingtyper: FØRSTEGANG og NY_VURDERING og sakstema: MEDLEMSKAP_LOVVALG")
        }
        if (hovedpart !== Aktoersroller.BRUKER) {
            throw FunksjonellException("Kan kun sende forvaltningsmelding for Aktoersroller: BRUKER")
        }
    }

    internal fun skalSetteSøknadslandOgPeriode(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Boolean =
        (sakstype == Sakstyper.EU_EOS && !saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(sakstype, sakstema, behandlingstema)
            && !saksbehandlingRegler.harIkkeYrkesaktivFlyt(sakstype, behandlingstema)
            && !saksbehandlingRegler.harPensjonistUføretrygdetFlyt(sakstype, behandlingstema))

    private fun validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding: MelosysEessiMelding) {
        if (eessiService.støtterAutomatiskBehandling(melosysEessiMelding)) {
            throw FunksjonellException("Journalpost med id ${melosysEessiMelding.journalpostId} skal ikke journalføres manuelt")
        }
    }

    private fun validerSedIkkeAlleredeTilknyttetAnnenSak(melosysEessiMelding: MelosysEessiMelding, tilknyttTilSaksnummer: String?) {
        val tilknyttetFagsak =
            eessiService.finnSakForRinasaksnummer(melosysEessiMelding.rinaSaksnummer).flatMap { fagsakService.finnFagsakFraArkivsakID(it) }

        if (tilknyttetFagsak.isPresent && tilknyttetFagsak.get().saksnummer != tilknyttTilSaksnummer) {
            throw FunksjonellException("RINA-sak ${melosysEessiMelding.rinaSaksnummer} er allerede tilknyttet ${tilknyttetFagsak.get().saksnummer}")
        }
    }

    private fun fellesValidering(journalfoeringDto: JournalfoeringDto, mottaksKanalErEessi: Boolean) {
        if (journalfoeringDto.journalpostID.isNullOrEmpty()) {
            throw FunksjonellException("JournalpostID mangler")
        }
        if (journalfoeringDto.oppgaveID.isNullOrEmpty()) {
            throw FunksjonellException("OppgaveID mangler")
        }
        if (!mottaksKanalErEessi) {
            if (journalfoeringDto.avsenderType != null && journalfoeringDto.avsenderID.isNullOrEmpty()) {
                throw FunksjonellException("AvsenderID er påkrevd når AvsenderType er satt")
            }
            if (!journalfoeringDto.avsenderID.isNullOrEmpty() && journalfoeringDto.avsenderType == null) {
                throw FunksjonellException("AvsenderType er påkrevd når AvsenderID er satt")
            }
            if (journalfoeringDto.avsenderNavn.isNullOrEmpty()) {
                throw FunksjonellException("AvsenderNavn mangler")
            }
        }
        if (journalfoeringDto.brukerID.isNullOrEmpty() && journalfoeringDto.virksomhetOrgnr.isNullOrEmpty()) {
            throw FunksjonellException("Både BrukerID og VirksomhetOrgnr mangler. Krever én")
        }
        if (!journalfoeringDto.brukerID.isNullOrEmpty() && !journalfoeringDto.virksomhetOrgnr.isNullOrEmpty()) {
            throw FunksjonellException("Både BrukerID og VirksomhetOrgnr finnes. Dette kan skape problemer. Velg én å journalføre dokumentet på.")
        }
        if (journalfoeringDto.hoveddokument.dokumentID.isNullOrBlank()) {
            throw FunksjonellException("DokumentID til hoveddokument mangler")
        }
        if (journalfoeringDto.hoveddokument.tittel.isNullOrBlank()) {
            throw FunksjonellException("Hoveddokument mangler tittel")
        }
        if (journalfoeringDto.vedlegg.any { it.dokumentID.isNullOrBlank() }) {
            throw FunksjonellException("DokumentID mangler for et vedlegg")
        }
        if (journalfoeringDto.vedlegg.any { it.tittel.isNullOrBlank() }) {
            throw FunksjonellException("Tittel mangler for et vedlegg")
        }
    }

    private fun validerSøknadsperiodeOgLand(journalfoeringDto: JournalfoeringOpprettDto) {
        val søknadsperiode = journalfoeringDto.fagsak.soknadsperiode ?: throw FunksjonellException("Søknadsperiode mangler")

        if (søknadsperiode.fom == null) {
            throw FunksjonellException("Søknadsperiodes fra og med dato mangler")
        }
        if (søknadsperiode.tom != null && søknadsperiode.fom.isAfter(søknadsperiode.tom)) {
            throw FunksjonellException("Fra og med dato kan ikke være etter til og med dato.")
        }

        if (!journalfoeringDto.fagsak.land.erGyldig()) {
            throw FunksjonellException("Informasjon om land er ugyldig")
        }

        val landkoder = journalfoeringDto.fagsak.land.landkoder
        if (landkoder.contains(null)) {
            throw FunksjonellException("Et søknadsland er null!")
        }

        val behandlingstema = Behandlingstema.valueOf(journalfoeringDto.behandlingstemaKode)

        if (behandlingstema == Behandlingstema.ARBEID_FLERE_LAND) {
            val flereLandUkjentHvilke = journalfoeringDto.fagsak.land.isFlereLandUkjentHvilke
            if (flereLandUkjentHvilke && landkoder.isNotEmpty()) {
                throw FunksjonellException("Det kan ikke være noen land for behandlingstema $behandlingstema dersom flereLandUkjentHvilke er valgt")
            }
            if (!flereLandUkjentHvilke && landkoder.size < 2) {
                throw FunksjonellException("Det er påkrevd med to eller flere land for behandlingstema $behandlingstema om ikke flereLandUkjentHvilke er valgt")
            }
        }

        if (behandlingstema in listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstema.UTSENDT_SELVSTENDIG) && landkoder.size != 1) {
            throw FunksjonellException("Kun ett søknadsland er tillatt for behandlingstema $behandlingstema")
        }
    }

    private fun validerSakstypeForUtenlandskTrygdemyndighetsland(sakstype: Sakstyper, landKodeAvsenderID: String) {
        val erEuEllerEøsLand = Landkoder.values().any { it.kode == landKodeAvsenderID }
        val erAvtaleland = Trygdeavtale_myndighetsland.values().any { it.kode == landKodeAvsenderID }

        if (erEuEllerEøsLand && !erAvtaleland && sakstype != Sakstyper.EU_EOS) {
            throw FunksjonellException("Sak for trygdemyndighet fra $landKodeAvsenderID skal være av type ${Sakstyper.EU_EOS}")
        }
        if (erAvtaleland && !erEuEllerEøsLand && sakstype != Sakstyper.TRYGDEAVTALE) {
            throw FunksjonellException("Sak for trygdemyndighet fra $landKodeAvsenderID skal være av type ${Sakstyper.TRYGDEAVTALE}")
        }
    }

    private fun journalføringGjelder(journalfoeringDto: JournalfoeringDto): Aktoersroller =
        if (journalfoeringDto.brukerID.isNullOrEmpty()) Aktoersroller.VIRKSOMHET else Aktoersroller.BRUKER

}
