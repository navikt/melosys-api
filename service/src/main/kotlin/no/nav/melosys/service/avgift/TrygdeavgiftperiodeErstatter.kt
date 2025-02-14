package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TrygdeavgiftperiodeErstatter(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional(readOnly = true)
    fun erPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
        behandlingsresultatID: Long
    ): Boolean {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val erPliktigMedlemskap = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktigMedlemskap && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattTrygdeavgiftsperioder(behandlingsresultatId: Long, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)
        nullstillTrygdeavgiftsperioder(behandlingsresultat)

        behandlingsresultat.medlemskapsperioder.forEach { mp ->
            trygdeavgiftsperioder.forEach { tp ->
                if (tp.grunnlagMedlemskapsperiode?.id == mp.id) {
                    mp.addTrygdeavgiftsperiode(tp)
                }
            }
        }
    }

    /*
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun erstattTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(
        behandlingsresultatId: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
    ): Set<Trygdeavgiftsperiode> {
        require(skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatId)

        val trygdeavgiftsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }.map { mp ->
            val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = skatteforholdsperioder.first().fom
                tomDato = skatteforholdsperioder.first().tom
                skatteplikttype = skatteforholdsperioder.first().skatteplikttype
            }

            val trygdeavgiftsperiode = Trygdeavgiftsperiode(
                periodeFra = mp.fom,
                periodeTil = mp.tom,
                trygdesats = BigDecimal.ZERO,
                trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
                grunnlagMedlemskapsperiode = mp,
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
            )

            trygdeavgiftsperiode
        }

        erstattTrygdeavgiftsperioder(behandlingsresultatId, trygdeavgiftsperioder)
        return trygdeavgiftsperioder.toSet()
    }

     */

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.clearTrygdeavgiftsperioder()
        }
    }
}
