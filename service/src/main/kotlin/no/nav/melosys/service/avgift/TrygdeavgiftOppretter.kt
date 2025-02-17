package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import java.math.BigDecimal
import java.util.*

object TrygdeavgiftOppretter {
    /*
    Vi kan benytte medlemskapsperioden til å opprette trygdeavgiftsperiode med et skatteforhold med samme periode som medlemskapet
    når det er pliktig medlemskap og Skattepliktig Ja.
     */
    fun skattepliktigTrygdeavgiftsperioderAvMedlemskapsperioder(
        medlemskapsperioder: Collection<Medlemskapsperiode>
    ): List<Trygdeavgiftsperiode> = medlemskapsperioder.map { mp -> opprettSkattepliktigTrygdeavgiftsperiode(mp) }


    fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat
    ): Trygdeavgiftsperiode {
        val medlemskapsperiodeID = response.grunnlag.medlemskapsperiodeId
        val skatteforholdsperiodeID = response.grunnlag.skatteforholdsperiodeId
        val inntektsperiodeID = response.grunnlag.inntektsperiodeId


        val grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { idToUUid(it.id) == medlemskapsperiodeID }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode $medlemskapsperiodeID")

        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == skatteforholdsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke skatteforholdsperiode $skatteforholdsperiodeID")

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
