package no.nav.melosys.service.avgift

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
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
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
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultat, skatteforholdsperioder, inntektsperioder)
        nullstillTrygdeavgiftsperioder(behandlingsresultat)

        return leggTilNyeTrygdeavgiftsperioder(behandlingsresultat, skatteforholdsperioder, inntektsperioder)
    }

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.trygdeavgiftsperioder.clear()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun leggTilNyeTrygdeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>
    ): Set<Trygdeavgiftsperiode> {
        if (erPliktigMedlemskapSkattepliktig(skatteforholdsperioder, inntektsperioder, behandlingsresultat)) {
            return leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(behandlingsresultat, skatteforholdsperioder)
        }

        val inntektsperioderMedUUID = inntektsperioder.map { Pair(UUID.randomUUID(), it) }
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }

        val skatteforholdsperioderMedUUID = skatteforholdsperioder.map { Pair(UUID.randomUUID(), it) }
        val skatteforholdsperiodeDtoList = skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }

        val beregnetTrygdeavgift = beregnTrygdeavgift(behandlingsresultat, skatteforholdsperiodeDtoList, inntektsperiodeDtoList)

        val nyeTrygdeavgiftsperioder = beregnetTrygdeavgift.map { response ->
            lagTrygdeavgiftsperiode(response, skatteforholdsperioderMedUUID, inntektsperioderMedUUID, behandlingsresultat)
        }.toSet()

        sjekkTrygdeavgiftSkalBetalesTilNav(behandlingsresultat, beregnetTrygdeavgift)
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        return nyeTrygdeavgiftsperioder
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

    private fun beregnTrygdeavgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioderTemp: List<SkatteforholdsperiodeDto>,
        inntektsPerioderTemp: List<InntektsperiodeDto>
    ): List<TrygdeavgiftsberegningResponse> {
        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)


        return trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftsberegningRequest(
                innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtoSet(),
                skatteforholdsperioderTemp.toSet(),
                inntektsPerioderTemp,
                foedselDato
            )
        )
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(
        behandlingsresultat: Behandlingsresultat,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>
    ) {
        val erAlleTrygdeavgiftNullBeløp = beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        check(!(skalKunBetalesTilSkatt && !erAlleTrygdeavgiftNullBeløp)) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val behandling = behandlingsresultat.behandling
        behandling.opprinneligBehandling?.let {
            return behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder
                ?: emptySet()
        }
        return emptySet()
    }

    private fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat
    ): Trygdeavgiftsperiode {
        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == response.grunnlag.skatteforholdsperiodeId }?.second

        val grunnlagInntekstperiode = inntektsperioderMedUUID
            .find { it.first == response.grunnlag.inntektsperiodeId }?.second

        val grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { idToUUid(it.id) == response.grunnlag.medlemskapsperiodeId }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode")

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
            grunnlagInntekstperiode = grunnlagInntekstperiode,
            grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode
        ).also { grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(it) }

        return trygdeavgiftsperiode
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

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
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
}
