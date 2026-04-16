package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.dokument.inntekt.Periode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.HelseutgiftDekkesPeriodeData

object OverlappendeHelseutgiftDekkesPerioderRegler {

    fun harOverlappendeMedlPeriode(
        medlemskapDokument: MedlemskapDokument,
        helseutgiftDekkesPeriodeData: HelseutgiftDekkesPeriodeData
    ): Boolean {
        return helseutgiftDekkesPeriodeData.nyeHelseutgiftDekkesPerioder.any { nyHelseutgiftDekkesPeriode ->
            val kontrollperiode = Periode(nyHelseutgiftDekkesPeriode.fomDato, nyHelseutgiftDekkesPeriode.tomDato)

            medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen()
                .filter { medlemsperiode -> PeriodestatusMedl.AVST.kode != medlemsperiode.status }
                .any { medlemsperiode -> PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.periode) }
        }
    }

    fun harOverlappendeHelseutgiftDekkesPeriode(
        helseutgiftDekkesPeriodeData: HelseutgiftDekkesPeriodeData
    ): Boolean {
        return helseutgiftDekkesPeriodeData.nyeHelseutgiftDekkesPerioder.any { nyHelseutgiftDekkesPeriode ->
            val kontrollperiode = Periode(nyHelseutgiftDekkesPeriode.fomDato, nyHelseutgiftDekkesPeriode.tomDato)

            helseutgiftDekkesPeriodeData.tidligereHelseutgiftDekkesPerioder.any { helseutgiftDekkesPeriode ->
                val tidligerePeriode = Periode(helseutgiftDekkesPeriode.fomDato, helseutgiftDekkesPeriode.tomDato)
                PeriodeRegler.periodeOverlapper(kontrollperiode, tidligerePeriode)
            }
        }
    }
}
