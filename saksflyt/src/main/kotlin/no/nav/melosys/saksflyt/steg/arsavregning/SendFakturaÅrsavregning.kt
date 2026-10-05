package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenClient
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.Innbetalingstype
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class SendFakturaÅrsavregning(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenClient: FaktureringskomponentenClient,
    private val pdlService: PersondataService,
    private val årsavregningService: ÅrsavregningService,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_FAKTURA_AARSAVREGNING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandlingsId = prosessinstans.hentBehandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)!!


        if (tilFaktureringBelopErStørreEllerLikMinimumBeløp(behandlingsresultat)) {
            val fakturaDto = mapFakturaserieDto(behandlingsresultat)
            val responseDto = faktureringskomponentenClient.lagFaktura(fakturaDto, saksbehandlerIdent)
            behandlingsresultat.fakturaserieReferanse = responseDto.fakturaserieReferanse
            behandlingsresultatService.lagre(behandlingsresultat)
            log.info("Oppretter årsavregningfaktura for behandling: $behandlingsId")
        } else {
            log.info("Belop til fakturering er mindre enn ${MINIMUM_BELØP_FAKTURERING.beløp} kr for behandling: $behandlingsId, faktura sendes ikke")
        }
    }

    private fun tilFaktureringBelopErStørreEllerLikMinimumBeløp(behandlingsresultat: Behandlingsresultat): Boolean {
        return behandlingsresultat.hentÅrsavregning().hentTilFaktureringBeloep.abs() >= MINIMUM_BELØP_FAKTURERING.beløp
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat): FakturaDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.hentId())
        val årsavregning = behandlingsresultat.hentÅrsavregning()
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato = FORMATTER.format(behandlingsresultat.hentVedtakMetadata().vedtaksdato)
        val (startDato, sluttDato) = finnFakturaperiode(behandlingsresultat)
        val startDatoFormatert = FORMATTER.format(startDato)
        val sluttDatoFormatert = FORMATTER.format(sluttDato)
        val harTidligereÅrsavregning = årsavregning.tidligereBehandlingsresultat?.behandling?.erÅrsavregning() ?: false
        val tidligereFakturertSum = (årsavregning.tidligereFakturertBeloep ?: BigDecimal.ZERO).add(
            årsavregning.innbetaltTrygdeavgift ?: BigDecimal.ZERO
        )

        return FakturaDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = if (harTidligereÅrsavregning) årsavregning.hentTidligereBehandlingsresultat.fakturaserieReferanse else null,
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING,
            referanseBruker = "Årsavregning datert $vedtaksdato",
            belop = årsavregning.hentTilFaktureringBeloep,
            startDato = startDato,
            sluttDato = sluttDato,
            beskrivelse = if (årsavregning.manueltAvgiftBeloep == null) {
                "Periode ${startDatoFormatert} - $sluttDatoFormatert, endelig beregnet trygdeavgift ${årsavregning.beregnetAvgiftBelop} - forskuddsvis" +
                    " betalt trygdeavgift $tidligereFakturertSum"
            } else "Årsavregning ${årsavregning.aar}" // TODO: Endre denne når fag har kommet fram til bedre begrep. Kanskje lage egen felt for "fakturalinjeBeskrivelse" i FakturaDto?
        )
    }

    /**
     * Perioden fakturaen gjelder, innenfor årsavregningsåret. Hentes fra årsavregningens egne perioder for året. Har den
     * ingen, fordi året er fjernet av en senere vurdering, brukes perioden som sist ble gjort opp for året: siste
     * årsavregning, ellers siste behandling med trygdeavgift. Uten perioder i saken gjelder fakturaen hele året.
     */
    private fun finnFakturaperiode(behandlingsresultat: Behandlingsresultat): Pair<LocalDate, LocalDate> {
        val år = behandlingsresultat.hentÅrsavregning().aar
        val førsteDagIÅret = LocalDate.of(år, 1, 1)
        val sisteDagIÅret = LocalDate.of(år, 12, 31)

        val perioder = perioderForÅr(behandlingsresultat, år).ifEmpty {
            // Dersom årsavregningen mangler avgiftspliktige perioder for året.
            // For eksempel hvis en NY_VURDERING har fjernet medlemskapsperioder for hele året.
            val gjeldende = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat.hentBehandling().fagsak.saksnummer,
                år,
                behandlingsresultat.vedtakMetadata?.vedtaksdato
            )
            listOfNotNull(gjeldende?.sisteÅrsavregning, gjeldende?.sisteBehandlingsresultatMedAvgift)
                .map { perioderForÅr(it, år) }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        }

        return (perioder.minOfOrNull { it.getFom() } ?: førsteDagIÅret) to
            (perioder.maxOfOrNull { it.getTom() ?: sisteDagIÅret } ?: sisteDagIÅret)
    }

    /** Trygdeavgiftsperiodene som overlapper året, ellers de innvilgede avgiftspliktige periodene som overlapper året. */
    private fun perioderForÅr(behandlingsresultat: Behandlingsresultat, år: Int): List<ErPeriode> =
        behandlingsresultat.trygdeavgiftsperioder
            .filter { it.overlapperMedÅr(år) }
            .ifEmpty { behandlingsresultat.finnAvgiftspliktigPerioder().filter { it.erInnvilget() && it.overlapperMedÅr(år) } }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
    }
}
