package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ErstattTrygdeavgiftsperioderService {
    fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig(
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
}
