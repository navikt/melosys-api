package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlappendeMedlemskapsperioderReglerTest {

    @Test
    void overlappendeMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2), LocalDate.EPOCH.minusYears(1)),
            null)
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5L)),
            null)
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(1)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_5() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_6() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_7() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument(),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1),
                null), null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeAvvistPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.AVST.getKode();
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeUavklartPeriode_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        ).isTrue();
    }

    @Test
    void overlappendeMedlemsperiodeFraSed_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeFraSed(
            medlemskapDokument, new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        ).isTrue();
    }


    @Test
    void harOverlappendeMedlemsperiodeFraSed_inklusivOverlappendePeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertFalse(erTattIKontroll);
    }

    @Test
    void harOverlappendeMedlemsperiodeFraSed_inklusivOverlappendePeriode_medEnDagOver_registrerTreff() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);

        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertTrue(erTattIKontroll);
    }

    @Test
    void overlappendeMedlemsperiode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeNyVurderingLovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        lovvalgsperiode.setMedlPeriodeID(123L);
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument, lovvalgsperiode, null)
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiode_overlappendePeriodeNyVurderingOpprinneligPeriodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode opprinneligLovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        opprinneligLovvalgsperiode.setMedlPeriodeID(123L);
        assertThat(OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            opprinneligLovvalgsperiode)
        ).isFalse();
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
