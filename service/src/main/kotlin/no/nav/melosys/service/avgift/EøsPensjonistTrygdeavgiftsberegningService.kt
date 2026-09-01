package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftClient
import no.nav.melosys.integrasjon.trygdeavgift.dto.BeregningsforklaringDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningResponse
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class EøsPensjonistTrygdeavgiftsberegningService(
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val persondataService: PersondataService,
    private val trygdeavgiftClient: TrygdeavgiftClient,
    private val unleash: Unleash
) {
    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
        dagensDato: LocalDate = LocalDate.now()
    ): Set<Trygdeavgiftsperiode> =
        beregnOgLagreTrygdeavgiftMedForklaring(behandlingID, skatteforholdsperioder, inntektsperioder, dagensDato)
            .trygdeavgiftsperioder

    /**
     * Som [beregnOgLagreTrygdeavgift], men returnerer i tillegg de distinkte
     * beregningsforklaringene fra beregningsmotoren. Forklaringene persisteres ikke –
     * de føres kun gjennom på PUT-veien (beregning) til frontend.
     */
    @Transactional
    fun beregnOgLagreTrygdeavgiftMedForklaring(
        behandlingID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
        dagensDato: LocalDate = LocalDate.now()
    ): BeregnetTrygdeavgiftMedForklaring {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val helseutgiftDekkesPerioder = behandlingsresultat.helseutgiftDekkesPerioder

        require(helseutgiftDekkesPerioder.isNotEmpty()) { "Ingen helseutgift dekkes perioder funnet for behandling $behandlingID" }

        EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
            helseutgiftDekkesPerioder.toList(),
            skatteforholdsperioder,
            inntektsperioder,
            behandlingsresultat,
            unleash,
            dagensDato
        )

        val resultat =
            lagNyeTrygdeavgiftsperioderMedForklaring(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        val nyeTrygdeavgiftsperioder = resultat.trygdeavgiftsperioder

        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(behandlingID, nyeTrygdeavgiftsperioder)

        behandlingsresultat.årsavregning?.let { årsavregning ->
            if (årsavregning.endeligAvgiftValg != EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT) {
                val totalAvgift = TotalbeløpBeregner.hentTotalavgift(nyeTrygdeavgiftsperioder)
                årsavregning.beregnetAvgiftBelop = totalAvgift
                if (totalAvgift != null) {
                    årsavregning.beregnTilFaktureringsBeloep()
                } else {
                    årsavregning.tilFaktureringBeloep = null
                }
            }
        }

        return BeregnetTrygdeavgiftMedForklaring(nyeTrygdeavgiftsperioder.toSet(), resultat.beregningsforklaringer)
    }

    private fun beregnTrygdeavgiftMedForklaring(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): EøsPensjonistBeregningsresultat {
        val helseutgiftDekkesPerioder = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPerioder(behandlingsresultat.hentBehandling().id)
        require(helseutgiftDekkesPerioder.isNotEmpty()) { "Ingen helseutgift dekkes perioder funnet" }

        val resultaterPerPeriode = helseutgiftDekkesPerioder.map { helseutgiftDekkesPeriode ->
            beregnTrygdeavgiftForEnkeltPeriode(behandlingsresultat, helseutgiftDekkesPeriode, skatteforholdsperioder, inntektsperioder, dagensDato)
                .also { resultat ->
                    resultat.trygdeavgiftsperioder.forEach { it.grunnlagHelseutgiftDekkesPeriode = helseutgiftDekkesPeriode }
                }
        }

        return EøsPensjonistBeregningsresultat(
            resultaterPerPeriode.flatMap { it.trygdeavgiftsperioder },
            slåSammenForklaringer(resultaterPerPeriode.flatMap { it.beregningsforklaringer })
        )
    }

    /**
     * Beregningsmotoren kalles én gang per helseutgiftDekkesPeriode, med inntektene filtrert til
     * den perioden. En behandling med flere perioder i samme år får derfor flere forklaringer for
     * det året. Web nøkler både kortet og koblingen fra satsen på (aar, inntektsgruppe), og ville
     * vist den første forklaringen for alle periodene i året – altså tall som ikke hører til
     * perioden. Slike (år, inntektsgruppe) utelates heller helt: da vises ingen forklaring,
     * slik det var før. (EØS-motoren gir i dag kun SAMLET, så det er i praksis hele året.)
     * Sorteringen gjør rekkefølgen stabil; periodene kommer uordnet fra repoet.
     *
     * Merk at likhet er BigDecimal-likhet, som er skala-sensitiv (7.9 != 7.90). To kall som gir
     * samme tall med ulik skala vil derfor telle som tvetydige og skjule forklaringen. Motoren
     * regner likt for like input, så det krever at kallene faktisk får ulikt grunnlag.
     */
    private fun slåSammenForklaringer(forklaringer: List<BeregningsforklaringDto>): List<BeregningsforklaringDto> =
        forklaringer.distinct()
            .groupBy { it.aar to it.inntektsgruppe }
            .filterValues { it.size == 1 }
            .values
            .flatten()
            .sortedWith(compareBy({ it.aar }, { it.inntektsgruppe }))

    private fun beregnTrygdeavgiftForEnkeltPeriode(
        behandlingsresultat: Behandlingsresultat,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate
    ): EøsPensjonistBeregningsresultat {
        // UUID brukes til å identifisere periodene som danner grunnlag for trygdeavgiftsberegningen
        val helseutgiftDekkesPeriodeDto = helseutgiftDekkesPeriode.tilHelseutgiftDekkesPeriodeDto()
        val filtrerteInntektsperioder = inntektsperioder.filter { inntektsperiode ->
            inntektsperiode.fomDato.isBefore(helseutgiftDekkesPeriode.tomDato) &&
                inntektsperiode.tomDato.isAfter(helseutgiftDekkesPeriode.fomDato)
        }
        val inntektsperioderMedUUID = filtrerteInntektsperioder.map { UUID.randomUUID() to it }
        val skatteforholdsperioderMedUUID = skatteforholdsperioder.map { UUID.randomUUID() to it }
        val skatteforholdsperiodeDtoSet =
            skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }.toSet()
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }
        val fagsak = behandlingService.hentBehandling(behandlingsresultat.hentId()).fagsak
        val foedselsdato = persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato

        val beregnetTrygdeavgiftList = trygdeavgiftClient.beregnTrygdeavgiftEosPensjonist(
            EøsPensjonistTrygdeavgiftsberegningRequest(
                helseutgiftDekkesPeriodeDto,
                skatteforholdsperiodeDtoSet,
                inntektsperiodeDtoList,
                foedselsdato
            )
        )

        val relevanteResponser = beregnetTrygdeavgiftList
            .filter { response ->
                !response.beregnetPeriode.periode.fom.isAfter(helseutgiftDekkesPeriode.tomDato) &&
                    !response.beregnetPeriode.periode.tom.isBefore(helseutgiftDekkesPeriode.fomDato)
            }

        return EøsPensjonistBeregningsresultat(
            relevanteResponser.map { beregnetAvgiftPerPeriode ->
                lagTrygdeavgiftsperiode(
                    beregnetAvgiftPerPeriode,
                    skatteforholdsperioderMedUUID,
                    inntektsperioderMedUUID,
                    helseutgiftDekkesPeriode
                )
            },
            relevanteResponser.mapNotNull { it.beregningsforklaring }.distinct()
        )
    }

    private fun lagTrygdeavgiftsperiode(
        response: EøsPensjonistTrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode
    ): Trygdeavgiftsperiode {
        val alleGrunnlag = response.grunnlagListe.ifEmpty { listOf(response.grunnlag) }
        val skatteforholdMap = skatteforholdsperioderMedUUID.toMap()
        val inntektsperiodeMap = inntektsperioderMedUUID.toMap()

        // Legacy FK-felt speiler response.grunnlag ("siste/eneste") for bakoverkompatibilitet
        val legacyGrunnlag = response.grunnlag

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger().avrundTilHelKroner(),
            grunnlagSkatteforholdTilNorge = skatteforholdMap[legacyGrunnlag.skatteforholdsperiodeId]
                ?: throw IllegalStateException("Fant ikke skatteforholdsperiode ${legacyGrunnlag.skatteforholdsperiodeId}"),
            grunnlagInntekstperiode = inntektsperiodeMap[legacyGrunnlag.inntektsperiodeId]
                ?: throw IllegalStateException("Fant ikke inntektsperiode ${legacyGrunnlag.inntektsperiodeId}"),
            beregningsregel = response.beregningsregel,
        )

        alleGrunnlag.forEach { grunnlagDto ->
            val grunnlagEntitet = TrygdeavgiftsperiodeGrunnlag(
                trygdeavgiftsperiode = trygdeavgiftsperiode,
                helseutgiftDekkesPeriode = helseutgiftDekkesPeriode,
                inntektsperiode = inntektsperiodeMap[grunnlagDto.inntektsperiodeId]
                    ?: throw IllegalStateException("Fant ikke inntektsperiode ${grunnlagDto.inntektsperiodeId}"),
                skatteforhold = skatteforholdMap[grunnlagDto.skatteforholdsperiodeId]
                    ?: throw IllegalStateException("Fant ikke skatteforholdsperiode ${grunnlagDto.skatteforholdsperiodeId}"),
            )
            trygdeavgiftsperiode.leggTilGrunnlag(grunnlagEntitet)
        }

        return trygdeavgiftsperiode
    }

    private fun lagNyeTrygdeavgiftsperioderMedForklaring(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): EøsPensjonistBeregningsresultat {

        if (erSkattepliktig(skatteforholdsperioder, inntektsperioder) && skatteforholdsperioder.size == 1) {
            // Skattepliktig-snarveien kaller ikke beregningsmotoren, og har derfor ingen forklaring.
            return EøsPensjonistBeregningsresultat(
                skattepliktigTrygdeavgiftsperioderAvAvgiftspliktigperioder(behandlingsresultat.finnAvgiftspliktigPerioder(), dagensDato),
                emptyList()
            )
        }

        val resultat = beregnTrygdeavgiftMedForklaring(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        sjekkTrygdeavgiftSkalBetalesTilNav(resultat.trygdeavgiftsperioder)

        return resultat
    }

    private fun erSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
    ): Boolean {
        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {

        val erAlleTrygdeavgiftNullBeløp =
            trygdeavgiftsperioder.all { it.trygdeavgiftsbeløpMd.hentVerdi().compareTo(BigDecimal.ZERO) == 0 }

        val skalKunBetalesTilSkatt = trygdeavgiftMottakerService
            .getTrygdeavgiftMottaker(trygdeavgiftsperioder) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        check(erAlleTrygdeavgiftNullBeløp || !skalKunBetalesTilSkatt) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .eøsPensjonistTrygdeavgiftsperioder
    }


    // Metoden ser ikke ut til å høre hjemme her
    @Transactional(readOnly = true)
    fun finnFakturamottakerNavn(behandlingID: Long): String {
        val fagsak = behandlingService.hentBehandling(behandlingID).fagsak
        fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            .let {
                if (it == null)
                    return persondataService.hentSammensattNavn(fagsak.hentBrukersAktørID())
                if (it.erPerson())
                    return persondataService.hentSammensattNavn(it.personIdent)
                return eregFasade.hentOrganisasjonNavn(it.orgnr)
            }
    }

    private fun skattepliktigTrygdeavgiftsperioderAvAvgiftspliktigperioder(
        avgiftspliktigperioder: Collection<AvgiftspliktigPeriode>,
        dagensDato: LocalDate = LocalDate.now()
    ): List<Trygdeavgiftsperiode> {
        val fraOgMedÅr = if (unleash.isEnabled(MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) dagensDato.year else null
        return avgiftspliktigperioder.flatMap { SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(it, fraOgMedÅr) }
    }
}

private data class EøsPensjonistBeregningsresultat(
    val trygdeavgiftsperioder: List<Trygdeavgiftsperiode>,
    val beregningsforklaringer: List<BeregningsforklaringDto>,
)
