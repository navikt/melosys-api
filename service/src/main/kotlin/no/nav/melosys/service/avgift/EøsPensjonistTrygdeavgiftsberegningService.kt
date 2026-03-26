package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftClient
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
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val helseutgiftDekkesPeriode = behandlingsresultat.helseutgiftDekkesPeriode

        EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
            helseutgiftDekkesPeriode!!,
            skatteforholdsperioder,
            inntektsperioder,
            behandlingsresultat,
            unleash,
            dagensDato
        )

        val nyeTrygdeavgiftsperioder =
            lagNyeTrygdeavgiftsperioder(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)

        trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(behandlingID, nyeTrygdeavgiftsperioder)

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

        return nyeTrygdeavgiftsperioder.toSet()
    }

    @Transactional(readOnly = true, noRollbackFor = [Throwable::class])
    fun beregnTrygdeavgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): List<Trygdeavgiftsperiode> {
        // UUID brukes til å identifisere periodene som danner grunnlag for trygdeavgiftsberegningen
        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(behandlingsresultat.hentBehandling().id)
        val helseutgiftDekkesPeriodeDto = helseutgiftDekkesPeriode!!.tilHelseutgiftDekkesPeriodeDto()
        val inntektsperioderMedUUID = inntektsperioder.map { UUID.randomUUID() to it }
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

        return beregnetTrygdeavgiftList.map { beregnetAvgiftPerPeriode ->
            lagTrygdeavgiftsperiode(
                beregnetAvgiftPerPeriode,
                skatteforholdsperioderMedUUID,
                inntektsperioderMedUUID,
                dagensDato
            )
        }
    }

    private fun lagTrygdeavgiftsperiode(
        response: EøsPensjonistTrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        dagensDato: LocalDate
    ): Trygdeavgiftsperiode {
        val alleGrunnlag = response.grunnlagListe ?: listOf(response.grunnlag)
        val beregningsregel = response.beregningsregel
            ?.let { Avgiftsberegningsregel.valueOf(it) }
            ?: Avgiftsberegningsregel.ORDINAER

        val skatteforholdMap = skatteforholdsperioderMedUUID.toMap()
        val inntektsperiodeMap = inntektsperioderMedUUID.toMap()
        val førsteGrunnlag = alleGrunnlag.first()

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagSkatteforholdTilNorge = skatteforholdMap[førsteGrunnlag.skatteforholdsperiodeId]
                ?: throw IllegalStateException("Fant ikke skatteforholdsperiode ${førsteGrunnlag.skatteforholdsperiodeId}"),
            grunnlagInntekstperiode = inntektsperiodeMap[førsteGrunnlag.inntektsperiodeId]
                ?: throw IllegalStateException("Fant ikke inntektsperiode ${førsteGrunnlag.inntektsperiodeId}"),
            beregningsregel = beregningsregel,
        )

        alleGrunnlag.forEach { grunnlagDto ->
            val grunnlagEntitet = TrygdeavgiftsperiodeGrunnlag(
                trygdeavgiftsperiode = trygdeavgiftsperiode,
                inntektsperiode = inntektsperiodeMap[grunnlagDto.inntektsperiodeId]
                    ?: throw IllegalStateException("Fant ikke inntektsperiode ${grunnlagDto.inntektsperiodeId}"),
                skatteforhold = skatteforholdMap[grunnlagDto.skatteforholdsperiodeId]
                    ?: throw IllegalStateException("Fant ikke skatteforholdsperiode ${grunnlagDto.skatteforholdsperiodeId}"),
            )
            trygdeavgiftsperiode.leggTilGrunnlag(grunnlagEntitet)
        }

        return trygdeavgiftsperiode
    }

    @Suppress("SpringTransactionalMethodCallsInspection") // warning på beregnTrygdeavgift ignoreres pga eksisterende transaksjon
    private fun lagNyeTrygdeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): List<Trygdeavgiftsperiode> {

        if (erSkattepliktig(skatteforholdsperioder, inntektsperioder) && skatteforholdsperioder.size == 1) {
            return skattepliktigTrygdeavgiftsperioderAvAvgiftspliktigperioder(behandlingsresultat.finnAvgiftspliktigPerioder(), dagensDato)
        }

        val nyeTrygdeavgiftsperioder = beregnTrygdeavgift(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        sjekkTrygdeavgiftSkalBetalesTilNav(nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder
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
            .hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder
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
