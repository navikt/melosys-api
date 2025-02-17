package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigDecimal

object TrygdeavgiftOppretter {
    /*
    Vi kan benytte medlemskapsperioden til å opprette trygdeavgiftsperiode med et skatteforhold med samme periode som medlemskapet
    når det er pliktig medlemskap og Skattepliktig Ja.
     */
    fun skattepliktigTrygdeavgiftsperioderAvMedlemskapsperioder(
        medlemskapsperioder: Collection<Medlemskapsperiode>
    ): List<Trygdeavgiftsperiode> = medlemskapsperioder.map { mp ->
        opprettSkattepliktigTrygdeavgiftsperiode(mp)
    }

    private fun opprettSkattepliktigTrygdeavgiftsperiode(medlemskapsperiode: Medlemskapsperiode): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            periodeFra = medlemskapsperiode.fom,
            periodeTil = medlemskapsperiode.tom,
            trygdesats = BigDecimal.ZERO,
            trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = medlemskapsperiode.fom
                tomDato = medlemskapsperiode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }

        )
    }
}
