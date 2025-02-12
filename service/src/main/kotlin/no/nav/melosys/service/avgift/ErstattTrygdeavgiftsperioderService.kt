package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ErstattTrygdeavgiftsperioderService(private val behandlingsresultatService: BehandlingsresultatService) {
    fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
    ): Set<Trygdeavgiftsperiode> {
        require(skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }
        nullstillTrygdeavgiftsperioder(behandlingsresultat)
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

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.clearTrygdeavgiftsperioder()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }
}
