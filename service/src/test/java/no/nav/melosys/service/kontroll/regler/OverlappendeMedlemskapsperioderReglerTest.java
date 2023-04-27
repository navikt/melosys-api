package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlappendeMedlemskapsperioderReglerTest {

    @Test
    void overlappendePeriode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2), LocalDate.EPOCH.minusYears(1)),
            null)
        );
    }

    @Test
    void overlappendePeriode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5L)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeUnntakPeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendeUnntaksperiode(
            lagMedlemskapsDokument("SWE"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsPeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }
    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_2() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_3() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_4() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_5() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_6() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_7() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1),
                null), null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeAvvistPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.AVST.getKode();
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeUavklartPeriode_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertTrue(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiodeFraSed_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        assertTrue(harOverlappendeMedlemsperiodeFraSed(
            medlemskapDokument, new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        );
    }


    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertFalse(erTattIKontroll);
    }

    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_medEnDagOver_registrerTreff() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);

        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertTrue(erTattIKontroll);
    }

    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_tidligerePeriodeOverlapperMedEnDag_under2År_ingenTreff() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 1));
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);

        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertFalse(erTattIKontroll);
    }

    @Test
    void overlappendePeriode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeNyVurderingLovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        lovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(harOverlappendePeriode(
            medlemskapDokument, lovvalgsperiode, null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeNyVurderingOpprinneligPeriodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode opprinneligLovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        opprinneligLovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            opprinneligLovvalgsperiode)
        );
    }

    @Test
    void harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland_medOverlappendePeriode_likLand_forventerIngenFeil() {

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.SE);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.of(2022, 1, 15), LocalDate.of(2022, 3, 1)));

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 2, 1));
        medlemsperiode.land = "SWE";
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);


        boolean harFeil = harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(sedDokument, medlemskapDokument);


        assertFalse(harFeil);
    }

    private static MedlemskapDokument lagMedlemskapsDokument(String land) {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        medlemsperiode.land = land;

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }

    private MedlemskapDokument lagUavklartMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        return medlemskapDokument;
    }


    private static Lovvalgsperiode lagLovvalgsPeriode(LocalDate fom, LocalDate tom) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);
        return lovvalgsperiode;
    }
}
