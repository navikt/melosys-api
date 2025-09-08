package no.nav.melosys.service.kontroll.regler.overlapp

import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.HelseutgiftDekkesPeriodeData
import no.nav.melosys.service.kontroll.regler.OverlappendeHelseutgiftDekkesPerioderRegler
import java.time.LocalDate
import kotlin.test.Test

internal class OverlappendeHelseutgiftDekkesPerioderReglerTest {
    val FOM = LocalDate.now()
    val TOM = LocalDate.now().plusDays(7)

    @Test
    fun `harOverlappendeMedlPeriode - overlapper med periode i MEDL - true`() {
        val medelskapDokument = lagMedlemskapsDokument(
            FOM.minusDays(2),
            TOM.minusDays(3)
        )
        val kontrollHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(FOM, TOM)

        OverlappendeHelseutgiftDekkesPerioderRegler.harOverlappendeMedlPeriode(
            medelskapDokument,
            HelseutgiftDekkesPeriodeData(
                nyHelseutgiftDekkesPeriode = kontrollHelseutgiftDekkesPeriode,
                tidligereHelseutgiftDekkesPerioder = emptyList()
            )
        ).shouldBeTrue()
    }

    @Test
    fun `harOverlappendeHelseutgiftDekkesPeriode - overlapper med helseutgiftDekkesPeriode fra andre fagsak`(){
        val tidligereHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(
            FOM.minusDays(2),
            TOM.minusDays(3)
        )

        val kontrollHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(FOM, TOM)

        OverlappendeHelseutgiftDekkesPerioderRegler.harOverlappendeHelseutgiftDekkesPeriode(
            HelseutgiftDekkesPeriodeData(
                nyHelseutgiftDekkesPeriode = kontrollHelseutgiftDekkesPeriode,
                tidligereHelseutgiftDekkesPerioder = listOf(tidligereHelseutgiftDekkesPeriode)
            )
        ).shouldBeTrue()

    }


    private fun lagHelseutgiftDekkesPeriode(fraOgMed: LocalDate, tilOgMed: LocalDate): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = Behandlingsresultat(),
            fomDato = fraOgMed,
            tomDato = tilOgMed,
            bostedLandkode = Land_iso2.NO
        )
    }

    private fun lagMedlemskapsDokument(fraOgMed: LocalDate, tilOgMed: LocalDate): MedlemskapDokument {
        return MedlemskapDokument().apply {
            medlemsperiode = listOf(
                Medlemsperiode(
                    periode = Periode(fraOgMed, tilOgMed)
                ).apply {
                    id = 1L
                    status = PeriodestatusMedl.GYLD.kode
                    this.land = "NOR"
                })
        }
    }
}
