package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
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
    private val trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
    private val unleash: Unleash
) {

    @Transactional(readOnly = true)
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
            behandlingsresultat,
            skatteforholdsperioder,
            inntektsperioder,
            unleash
        )

        val nyeTrygdeavgiftsperioder =
            lagNyeTrygeavgiftsperioder(behandlingsresultat, skatteforholdsperioder, inntektsperioder)
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(behandlingsresultatID, nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder.toSet()
    }

    @Suppress("SpringTransactionalMethodCallsInspection") // warning på beregnTrygdeavgift ignoreres pga eksisterende transaksjon
    private fun lagNyeTrygeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>
    ): List<Trygdeavgiftsperiode> {
        if (erPliktigMedlemskapSkattepliktig(skatteforholdsperioder, inntektsperioder, behandlingsresultat)) {
            require(behandlingsresultat.medlemskapsperioder.size == 1 && skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }
            return TrygdeavgiftOppretter.skattepliktigTrygdeavgiftsperioderAvMedlemskapsperioder(behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() })
        }

        val nyeTrygdeavgiftsperioder = beregnTrygdeavgift(behandlingsresultat, skatteforholdsperioder, inntektsperioder)
        sjekkTrygdeavgiftSkalBetalesTilNav(nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder
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

    @Transactional(readOnly = true, noRollbackFor = [Throwable::class])
    fun beregnTrygdeavgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>
    ): List<Trygdeavgiftsperiode> {
        // UUID brukes til å identifisere periodene som danner grunnlag for trygdeavgiftsberegningen
        val inntektsperioderMedUUID = inntektsperioder.map { UUID.randomUUID() to it }
        val skatteforholdsperioderMedUUID = skatteforholdsperioder.map { UUID.randomUUID() to it }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        val skatteforholdsperiodeDtoSet =
            skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }.toSet()
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)

        val beregnetTrygdeavgiftList = trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftsberegningRequest(
                innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtoSet(),
                skatteforholdsperiodeDtoSet,
                inntektsperiodeDtoList,
                foedselDato
            )
        )

        return beregnetTrygdeavgiftList.map { beregnetAvgiftPerPeriode ->
            TrygdeavgiftOppretter.lagTrygdeavgiftsperiode(
                beregnetAvgiftPerPeriode,
                skatteforholdsperioderMedUUID,
                inntektsperioderMedUUID,
                behandlingsresultat
            )
        }
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(
        behandlingsresultatID: Long,
        medlemskapsperioder: List<Medlemskapsperiode>
    ): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val erAlleTrygdeavgiftNullBeløp =
            trygdeavgiftsperioder.all { it.trygdeavgiftsbeløpMd.verdi.compareTo(BigDecimal.ZERO) == 0 }

        val skalKunBetalesTilSkatt = trygdeavgiftMottakerService
            .getTrygdeavgiftMottaker(trygdeavgiftsperioder) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        check(erAlleTrygdeavgiftNullBeløp || !skalKunBetalesTilSkatt) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
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
}
