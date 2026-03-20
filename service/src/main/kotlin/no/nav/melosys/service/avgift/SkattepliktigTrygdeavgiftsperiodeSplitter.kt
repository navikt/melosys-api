package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigDecimal
import java.time.LocalDate

object SkattepliktigTrygdeavgiftsperiodeSplitter {

    /**
     * Oppretter én [Trygdeavgiftsperiode] per kalenderår som avgiftspliktigperioden spenner over.
     * Nødvendig fordi trygdeavgiftsperioder ikke kan gå over et årsskifte, mens
     * avgiftspliktigperioder (f.eks. fra pliktig bestemmelse) kan strekke seg over flere år.
     *
     * @param fraOgMedÅr Hvis satt, filtreres perioder i tidligere år bort (toggle-styrt).
     */
    fun splittPåÅr(avgiftspliktigperiode: AvgiftspliktigPeriode, fraOgMedÅr: Int? = null): List<Trygdeavgiftsperiode> {
        val totalFom = avgiftspliktigperiode.fom
        val totalTom = avgiftspliktigperiode.tom

        return (totalFom.year..totalTom.year)
            .filter { år -> fraOgMedÅr == null || år >= fraOgMedÅr }
            .map { år ->
                val periodeFom = if (totalFom.year == år) totalFom else LocalDate.of(år, 1, 1)
                val periodeTom = if (totalTom.year == år) totalTom else LocalDate.of(år, 12, 31)
                opprettSkattepliktigTrygdeavgiftsperiode(avgiftspliktigperiode, periodeFom, periodeTom)
            }
    }

    private fun opprettSkattepliktigTrygdeavgiftsperiode(
        avgiftspliktigperiode: AvgiftspliktigPeriode,
        fom: LocalDate,
        tom: LocalDate
    ): Trygdeavgiftsperiode {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = fom,
            periodeTil = tom,
            trygdesats = BigDecimal.ZERO,
            trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = fom
                tomDato = tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )
        trygdeavgiftsperiode.addGrunnlag(avgiftspliktigperiode)
        return trygdeavgiftsperiode
    }
}
