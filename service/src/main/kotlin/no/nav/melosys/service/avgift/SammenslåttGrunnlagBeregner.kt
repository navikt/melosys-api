package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.AntallMdBeregner
import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Inntekt og arbeidsgiveravgift for en trygdeavgiftsperiode, utledet fra alle inntektene bak raden.
 *
 * Ved 25 %-regelen slår beregningen flere inntekter sammen til én rad. Legacy-feltet
 * [Trygdeavgiftsperiode.grunnlagInntekstperiode] peker da på kun én av dem, mens
 * [Trygdeavgiftsperiode.grunnlagListe] inneholder alle.
 */
object SammenslåttGrunnlagBeregner {

    /**
     * Bruttoinntekt per måned for raden.
     *
     * Er flere inntekter slått sammen, vektes hver av dem med hvor mange måneder den overlapper
     * raden: 9 000 kr/md og 2 000 kr/md hele året gir 11 000 kr/md, mens 9 000 kr/md jan–jun og
     * 2 000 kr/md jul–des gir 5 500 kr/md.
     */
    fun bruttoinntektPerMd(trygdeavgiftsperiode: Trygdeavgiftsperiode, verdiAvrundet: Boolean = false): BigDecimal {
        val måneder = månederI(trygdeavgiftsperiode.periodeFra, trygdeavgiftsperiode.periodeTil)
        if (måneder <= BigDecimal.ZERO) return BigDecimal.ZERO

        val perMåned = inntektForRad(trygdeavgiftsperiode).divide(måneder, 2, RoundingMode.HALF_UP)
        return if (verdiAvrundet) perMåned.setScale(0, RoundingMode.HALF_UP) else perMåned
    }

    /**
     * Om arbeidsgiveravgift betales til Skatteetaten.
     *
     * Null når de sammenslåtte inntektene ikke er enige — da finnes det ikke ett sant svar for raden.
     */
    fun arbeidsgiversavgiftBetales(trygdeavgiftsperiode: Trygdeavgiftsperiode): Boolean? =
        distinkteInntektsperioder(trygdeavgiftsperiode)
            ?.map { it.isArbeidsgiversavgiftBetalesTilSkatt }
            ?.distinct()
            ?.singleOrNull()

    /** Samlet bruttoinntekt for alle radene, brukt som «Brutto årsinntekt» i årsavregningen. */
    fun totalinntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal =
        trygdeavgiftsperioder.sumOf { inntektForRad(it) }

    /**
     * Kroner for hele raden.
     *
     * Med én inntekt dekker den raden, og hele radens lengde brukes — beregningen kan gi en rad som
     * ikke overlapper inntektsperioden den stammer fra. Med flere inntekter vektes hver av dem med
     * sin egen overlapp mot raden.
     */
    private fun inntektForRad(trygdeavgiftsperiode: Trygdeavgiftsperiode): BigDecimal {
        val inntektsperioder = distinkteInntektsperioder(trygdeavgiftsperiode)
            ?: return BigDecimal.ZERO

        val radensMåneder = månederI(trygdeavgiftsperiode.periodeFra, trygdeavgiftsperiode.periodeTil)

        inntektsperioder.singleOrNull()?.let { return andelAvInntekt(it, radensMåneder) }

        return inntektsperioder.sumOf { andelAvInntekt(it, overlappendeMåneder(it, trygdeavgiftsperiode)) }
    }

    /**
     * Kroner for et gitt antall måneder av inntekten. For et totalbeløp fordeles det over
     * inntektsperiodens egen lengde. Divisjonen gjøres til slutt, så et årsbeløp som dekker hele
     * raden kommer ut uendret.
     */
    private fun andelAvInntekt(inntektsperiode: Inntektsperiode, måneder: BigDecimal): BigDecimal {
        if (måneder <= BigDecimal.ZERO) return BigDecimal.ZERO

        if (inntektsperiode.erMaanedsbelop()) {
            return inntektsperiode.avgiftspliktigMndInntekt.hentVerdi() * måneder
        }

        val egneMåneder = månederI(inntektsperiode.fomDato, inntektsperiode.tomDato)
        if (egneMåneder <= BigDecimal.ZERO) return BigDecimal.ZERO

        return (inntektsperiode.avgiftspliktigTotalinntekt.hentVerdi() * måneder)
            .divide(egneMåneder, 2, RoundingMode.HALF_UP)
    }

    /**
     * Samme inntekt kan ligge flere ganger i grunnlagslista fordi beregningen periodiserer på
     * skatteforhold og årsskifte. Faller tilbake til legacy-feltet for rader lagret før lista fantes.
     */
    private fun distinkteInntektsperioder(trygdeavgiftsperiode: Trygdeavgiftsperiode): List<Inntektsperiode>? =
        trygdeavgiftsperiode.grunnlagListe
            .map { it.inntektsperiode }
            .distinctBy { it.id ?: it }
            .ifEmpty { listOfNotNull(trygdeavgiftsperiode.grunnlagInntekstperiode) }
            .ifEmpty { return null }

    private fun overlappendeMåneder(
        inntektsperiode: Inntektsperiode,
        trygdeavgiftsperiode: Trygdeavgiftsperiode
    ): BigDecimal {
        val fom = maxOf(inntektsperiode.fomDato, trygdeavgiftsperiode.periodeFra)
        val tom = minOf(inntektsperiode.tomDato, trygdeavgiftsperiode.periodeTil)

        return if (fom.isAfter(tom)) BigDecimal.ZERO else månederI(fom, tom)
    }

    private fun månederI(fom: LocalDate, tom: LocalDate): BigDecimal =
        AntallMdBeregner(fom, tom).beregn()
}
