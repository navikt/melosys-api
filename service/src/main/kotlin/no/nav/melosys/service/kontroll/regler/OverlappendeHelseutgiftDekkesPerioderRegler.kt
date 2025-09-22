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
        val nyHelseutgiftDekkesPeriode = helseutgiftDekkesPeriodeData.nyHelseutgiftDekkesPeriode
        val kontrollperiode = Periode(nyHelseutgiftDekkesPeriode.fomDato, nyHelseutgiftDekkesPeriode.tomDato)

        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen()
            .filter { medlemsperiode -> PeriodestatusMedl.AVST.kode != medlemsperiode.status }
            .any { medlemsperiode ->
                val kontrollErPeriode = kontrollperiode.tilErPeriode()
                val medlemsErPeriode = medlemsperiode.periode
                kontrollErPeriode != null && medlemsErPeriode != null &&
                PeriodeRegler.periodeOverlapper(kontrollErPeriode, medlemsErPeriode)
            }
    }

    fun harOverlappendeHelseutgiftDekkesPeriode(
        helseutgiftDekkesPeriodeData: HelseutgiftDekkesPeriodeData
    ): Boolean {
        val nyHelseutgiftDekkesPeriode = helseutgiftDekkesPeriodeData.nyHelseutgiftDekkesPeriode
        val kontrollperiode = Periode(nyHelseutgiftDekkesPeriode.fomDato, nyHelseutgiftDekkesPeriode.tomDato)

        return helseutgiftDekkesPeriodeData.tidligereHelseutgiftDekkesPerioder.any { helseutgiftDekkesPeriode ->
            val tidligerePeriode = Periode(helseutgiftDekkesPeriode.fomDato, helseutgiftDekkesPeriode.tomDato)
            val kontrollErPeriode = kontrollperiode.tilErPeriode()
            val tidligereErPeriode = tidligerePeriode.tilErPeriode()
            kontrollErPeriode != null && tidligereErPeriode != null &&
            PeriodeRegler.periodeOverlapper(kontrollErPeriode, tidligereErPeriode)
        }
    }
}
