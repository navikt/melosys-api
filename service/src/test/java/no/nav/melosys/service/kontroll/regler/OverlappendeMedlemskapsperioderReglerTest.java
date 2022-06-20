package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlappendeMedlemskapsperioderReglerTest {

    @Test
    void overlappendeMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2), LocalDate.EPOCH.minusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5L)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_5() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_6() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_7() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1),
                null), null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeAvvistPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.AVST.getKode();
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeUavklartPeriode_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiodeFraSed_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeFraSed(
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
    void overlappendeMedlemsperiode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeNyVurderingLovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        lovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument, lovvalgsperiode, null)
        );
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeNyVurderingOpprinneligPeriodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode opprinneligLovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        opprinneligLovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            opprinneligLovvalgsperiode)
        );
    }

    private static MedlemskapDokument lagMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }

    private MedlemskapDokument lagUavklartMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
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
