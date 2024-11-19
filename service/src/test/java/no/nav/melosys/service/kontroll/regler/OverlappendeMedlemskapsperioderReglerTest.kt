package no.nav.melosys.service.kontroll.regler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverlappendeMedlemskapsperioderReglerTest {
    @Test
    fun overlappendePeriode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2), LocalDate.EPOCH.minusYears(1)),
            null
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5L)),
            null
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_1() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendeUnntakPeriode_overlappendePeriode_registrerTreff_1() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendeUnntaksperiode(
            lagMedlemskapsDokument("SWE"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendeMedlemsPeriode_overlappendePeriode_registrerTreff_1() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_2() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_3() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_4() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(1)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_5() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_6() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriode_registrerTreff_7() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriodeOgTomErNull_registrerTreff() {
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(
                LocalDate.EPOCH.minusYears(1),
                null
            ), null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendePeriode_overlappendePeriodeAvvistPeriode_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].status = PeriodestatusMedl.AVST.kode
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_overlappendePeriodeUavklartPeriode_registrerTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].status = PeriodestatusMedl.UAVK.kode
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null
        ).shouldBeTrue()
    }

    @Test
    fun overlappendeMedlemsperiodeFraSed_overlappendePeriodeErUAVKL_registrerTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].status = PeriodestatusMedl.UAVK.kode
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeFraSed(
            medlemskapDokument, Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        ).shouldBeTrue()
    }

    @Test
    fun harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].apply { status = PeriodestatusMedl.UAVK.kode }
        }
        val kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed(
            medlemskapDokument,
            kontrollperiode
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_medEnDagOver_registrerTreff() {
        val medlemskapDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(Medlemsperiode(periode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1))).apply {
                status = PeriodestatusMedl.UAVK.kode
            })
        }
        val kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed(
            medlemskapDokument,
            kontrollperiode
        ).shouldBeTrue()
    }

    @Test
    fun harOverlappendeMedlemsperiodeMerEnn1DagFraSed_tidligerePeriodeOverlapperMedEnDag_under2År_ingenTreff() {
        val medlemskapDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(Medlemsperiode(periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 1))).apply {
                status = PeriodestatusMedl.GYLD.kode
            })
        }
        val kontrollperiode = lagLovvalgsPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))
        OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed(
            medlemskapDokument,
            kontrollperiode
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_kildeLånekassen_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].kilde = "LAANEKASSEN"
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_overlappendePeriodeNyVurderingLovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].id = 123L
        }
        val lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)).apply {
            medlPeriodeID = 123L
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument, lovvalgsperiode, null
        ).shouldBeFalse()
    }

    @Test
    fun overlappendePeriode_overlappendePeriodeNyVurderingOpprinneligPeriodeHarSammeMedlPeriodeID_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].id = 123L
        }
        val opprinneligLovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)).apply {
            medlPeriodeID = 123L
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            opprinneligLovvalgsperiode
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland_medOverlappendePeriode_likLand_forventerIngenFeil() {
        val sedDokument = SedDokument().apply {
            lovvalgslandKode = Landkoder.SE
            lovvalgsperiode = Periode(
                LocalDate.of(2022, 1, 15),
                LocalDate.of(2022, 3, 1)
            )
        }
        val medlemskapDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(Medlemsperiode(periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 2, 1))).apply {
                status = PeriodestatusMedl.GYLD.kode
                land = "SWE"
            })
        }
        OverlappendeMedlemskapsperioderRegler.harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(
            sedDokument,
            medlemskapDokument
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePeriode_kildeLånekassen_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].kilde = "LAANEKASSEN"
        }
        val kontrollMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        )
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePeriode_overlappendePeriodeMedSammeMedlPeriodeID_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].id = 123L
        }
        val kontrollMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)).apply { medlPeriodeID = 123L }
        )
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePeriode_flerePerioderIngenOverlapp_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        val kontrollMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5)),
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(6), LocalDate.EPOCH.plusYears(7))
        )
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePeriode_flerePerioderMedOverlapp_treff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        val kontrollMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(3))
        )
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeTrue()
    }

    @Test
    fun harOverlappendePeriode_perioderMedForskuddsvisFakturering_treff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        val kontrollMedlemskapsperioderMedAvgift = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
        )
        val tidligereMedlemskapsperioderMedAvgift = listOf(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
        )

        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriodeMedForskuddsvisFakturering(
            medlemskapDokument,
            MedlemskapsperiodeData(emptyList(), emptyList(), kontrollMedlemskapsperioderMedAvgift, tidligereMedlemskapsperioderMedAvgift)
        ).shouldBeTrue()
    }

    @Test
    fun harOverlappendePeriode_periodeMedAvstStatus_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR").apply {
            getMedlemsperiode()[0].status = "AVST"
        }
        val kontrollMedlemskapsperioder = listOf(lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeFalse()
    }

    @Test
    fun harOverlappendePeriode_overlappendePeriodeMedUlikMedlPeriodeID_treff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        val kontrollMedlemskapsperioder = listOf(
            lagMedlemskapsperiode(
                LocalDate.EPOCH.plusMonths(6),
                LocalDate.EPOCH.plusYears(3),
            ).apply { medlPeriodeID = 124L }
        )
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeTrue()
    }

    @Test
    fun harOverlappendePeriode_nullKontrollListe_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        shouldThrow<NullPointerException> {
            OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
                medlemskapDokument,
                null
            )
        }
    }

    @Test
    fun harOverlappendePeriode_tomKontrollListe_ingenTreff() {
        val medlemskapDokument = lagMedlemskapsDokument("NOR")
        val kontrollMedlemskapsperioder = emptyList<Medlemskapsperiode>()
        OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            MedlemskapsperiodeData(kontrollMedlemskapsperioder, emptyList())
        ).shouldBeFalse()
    }

    private fun lagMedlemskapsperiode(fraOgMed: LocalDate, tilOgMed: LocalDate): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            id = 1L
            fom = fraOgMed
            tom = tilOgMed
            innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        }
    }

    private fun lagMedlemskapsperiodeListe(vararg medlemskapsperioder: Medlemskapsperiode): List<Medlemskapsperiode> {
        return listOf(*medlemskapsperioder)
    }

    private fun lagMedlemskapsDokument(land: String): MedlemskapDokument {
        return MedlemskapDokument().apply {
            medlemsperiode = listOf(Medlemsperiode(periode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))).apply {
                id = 1L
                status = PeriodestatusMedl.GYLD.kode
                this.land = land
            })
        }
    }

    private fun lagLovvalgsPeriode(fom: LocalDate, tom: LocalDate?): Lovvalgsperiode {
        return Lovvalgsperiode().apply {
            this.fom = fom
            this.tom = tom
        }
    }
}
