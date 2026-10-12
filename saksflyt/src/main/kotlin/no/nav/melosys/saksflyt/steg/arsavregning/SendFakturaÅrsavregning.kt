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
import org.threeten.extra.LocalDateRange
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
        val fakturaperiode = finnFakturaperiode(behandlingsresultat)
        val startDato = fakturaperiode.start
        val sluttDato = fakturaperiode.endInclusive
        val startDatoFormatert = FORMATTER.format(startDato)
        val sluttDatoFormatert = FORMATTER.format(sluttDato)
        val harTidligereÅrsavregning = årsavregning.tidligereBehandlingsresultat?.behandling?.erÅrsavregning() ?: false
        val tidligereBetaltTotalt = årsavregning.hentTidligereBetaltTotalt

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
                    " betalt trygdeavgift $tidligereBetaltTotalt"
            } else "Årsavregning ${årsavregning.aar}" // TODO: Endre denne når fag har kommet fram til bedre begrep. Kanskje lage egen felt for "fakturalinjeBeskrivelse" i FakturaDto?
        )
    }

    /**
     * Perioden fakturaen gjelder, innenfor årsavregningsåret. Hentes fra årsavregningens egne perioder for året. Har den
     * ingen, fordi året er fjernet av en senere vurdering, brukes perioden som sist ble gjort opp for året: siste
     * årsavregning, ellers siste behandling med trygdeavgift. Uten perioder i saken gjelder fakturaen hele året.
     */
    private fun finnFakturaperiode(behandlingsresultat: Behandlingsresultat): LocalDateRange {
        val år = behandlingsresultat.hentÅrsavregning().aar
        val perioder = perioderIÅret(behandlingsresultat, år)
            .ifEmpty { perioderFraSisteOppgjørForÅret(behandlingsresultat, år) }

        if (perioder.isEmpty()) return heleÅret(år)
        return LocalDateRange.ofClosed(perioder.minOf { it.start }, perioder.maxOf { it.endInclusive })
    }

    /**
     * Brukes når årsavregningen mangler perioder for året, f.eks. fordi en NY_VURDERING har fjernet dem.
     * sisteBehandlingsresultatMedAvgiftspliktigPeriode brukes ikke med vilje: den kan være behandlingen som fjernet
     * året, og har da bare perioder for andre år.
     */
    private fun perioderFraSisteOppgjørForÅret(behandlingsresultat: Behandlingsresultat, år: Int): List<LocalDateRange> {
        val gjeldende = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
            behandlingsresultat.hentBehandling().fagsak.saksnummer,
            år,
            behandlingsresultat.vedtakMetadata?.vedtaksdato,
        ) ?: return emptyList()

        return listOfNotNull(gjeldende.sisteÅrsavregning, gjeldende.sisteBehandlingsresultatMedAvgift)
            .map { perioderIÅret(it, år) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    /**
     * Delen av hver periode som ligger i året. Trygdeavgiftsperiodene brukes, ellers de innvilgede avgiftspliktige
     * periodene. Trygdeavgiftsperiodene er alltid delt per år, men de avgiftspliktige periodene kan gå over
     * årsskiftet. Derfor avkortes hver periode til året, så fakturaperioden ikke havner utenfor året.
     */
    private fun perioderIÅret(behandlingsresultat: Behandlingsresultat, år: Int): List<LocalDateRange> {
        val heleÅret = heleÅret(år)
        val trygdeavgiftsperioder = behandlingsresultat.trygdeavgiftsperioder.filter { it.overlapperMedÅr(år) }
        val perioder: List<ErPeriode> = trygdeavgiftsperioder.ifEmpty {
            behandlingsresultat.finnAvgiftspliktigPerioder().filter { it.erInnvilget() && it.overlapperMedÅr(år) }
        }
        // Filteret over sikrer overlapp med året, så intersection kaster ikke
        return perioder.map { LocalDateRange.ofClosed(it.fom, it.tom ?: heleÅret.endInclusive).intersection(heleÅret) }
    }

    private fun heleÅret(år: Int): LocalDateRange = LocalDateRange.ofClosed(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31))

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
    }
}
