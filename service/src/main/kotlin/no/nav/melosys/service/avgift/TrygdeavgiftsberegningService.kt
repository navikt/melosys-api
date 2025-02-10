package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService(
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
    private val unleash: Unleash
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultat, skatteforholdsperioder, inntektsperioder, unleash)

        if (erPliktigMedlemskapSkattepliktig(skatteforholdsperioder, inntektsperioder, behandlingsresultat)) {
            nullstillTrygdeavgiftsperioder(behandlingsresultat)
            return leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(behandlingsresultat, skatteforholdsperioder)
        }

        nullstillTrygdeavgiftsperioder(behandlingsresultat)
        val nyeTrygdeavgiftsperioder =
            beregnTrygdeavgift(behandlingsresultat.id, behandlingsresultat.medlemskapsperioder, skatteforholdsperioder, inntektsperioder)
        // Knytter trygdeavgiftsperiodene til deres medlemskapsperiode før sjekken om trygdeavgift skal betales til Nav
        nyeTrygdeavgiftsperioder.forEach { it.grunnlagMedlemskapsperiodeNotNull.addTrygdeavgiftsperiode(it) }

        sjekkTrygdeavgiftSkalBetalesTilNav(behandlingsresultat, nyeTrygdeavgiftsperioder)
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)


        return nyeTrygdeavgiftsperioder.toSet()
    }

    @Transactional(readOnly = true)
    fun beregnTrygdeavgift(
        behandlingsresultatId: Long,
        medlemskapsperioder: Collection<Medlemskapsperiode>,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>
    ): List<Trygdeavgiftsperiode> {
        // UUID brukes til å identifisere periodene som danner grunnlag for trygdeavgiftsberegningen
        val inntektsperioderMedUUID = inntektsperioder.map { UUID.randomUUID() to it }
        val skatteforholdsperioderMedUUID = skatteforholdsperioder.map { UUID.randomUUID() to it }

        val innvilgedeMedlemskapsperioder = medlemskapsperioder.filter { it.erInnvilget() }
        val skatteforholdsperiodeDtoSet = skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }.toSet()
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatId, innvilgedeMedlemskapsperioder)

        val beregnetTrygdeavgiftList = trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftsberegningRequest(
                innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtoSet(),
                skatteforholdsperiodeDtoSet,
                inntektsperiodeDtoList,
                foedselDato
            )
        )

        return beregnetTrygdeavgiftList.map { beregnetAvgiftPerPeriode ->
            lagTrygdeavgiftsperiode(
                beregnetAvgiftPerPeriode,
                skatteforholdsperioderMedUUID,
                inntektsperioderMedUUID,
                medlemskapsperioder
            )
        }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingID: Long): Set<Trygdeavgiftsperiode> {
        val behandling = behandlingService.hentBehandling(behandlingID)

        return behandling.opprinneligBehandling?.let {
            behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder
        } ?: emptySet()
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

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.clearTrygdeavgiftsperioder()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun erPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
        behandlingsresultat: Behandlingsresultat
    ): Boolean {
        val erPliktigMedlemskap = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktigMedlemskap && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    private fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
    ): Set<Trygdeavgiftsperiode> {
        require(skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }
        val result = mutableSetOf<Trygdeavgiftsperiode>()

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        innvilgedeMedlemskapsperioder.forEach {
            val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = skatteforholdsperioder.first().fom
                tomDato = skatteforholdsperioder.first().tom
                skatteplikttype = skatteforholdsperioder.first().skatteplikttype
            }

            val trygdeavgiftsperiode = Trygdeavgiftsperiode(
                periodeFra = it.fom,
                periodeTil = it.tom,
                trygdesats = BigDecimal.ZERO,
                trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
                grunnlagMedlemskapsperiode = it,
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
            )

            it.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
            result.add(trygdeavgiftsperiode)
        }

        return result.toSet()
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        medlemskapsperioder: Collection<Medlemskapsperiode>
    ): Trygdeavgiftsperiode {
        val medlemskapsperiodeID = response.grunnlag.medlemskapsperiodeId
        val grunnlagMedlemskapsperiode = medlemskapsperioder
            .firstOrNull { idToUUid(it.id) == medlemskapsperiodeID }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode $medlemskapsperiodeID")

        val skatteforholdsperiodeID = response.grunnlag.skatteforholdsperiodeId
        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == skatteforholdsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke skatteforholdsperiode $skatteforholdsperiodeID")

        val inntektsperiodeID = response.grunnlag.inntektsperiodeId
        val grunnlagInntekstperiode = inntektsperioderMedUUID
            .find { it.first == inntektsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke inntektsperiode $inntektsperiodeID")

        return Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
            grunnlagInntekstperiode = grunnlagInntekstperiode
        )
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(
        behandlingsresultat: Behandlingsresultat,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ) {
        val erAlleTrygdeavgiftNullBeløp = trygdeavgiftsperioder.all { it.trygdeavgiftsbeløpMd.verdi.compareTo(BigDecimal.ZERO) == 0 }
        check(erAlleTrygdeavgiftNullBeløp || !skalKunBetalesTilSkatt(behandlingsresultat)) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
    }

    private fun skalKunBetalesTilSkatt(behandlingsresultat: Behandlingsresultat) =
        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
}
