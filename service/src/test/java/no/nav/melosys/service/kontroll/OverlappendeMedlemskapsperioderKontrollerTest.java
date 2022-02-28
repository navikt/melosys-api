package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OverlappendeMedlemskapsperioderKontrollerTest {

    @Test
    void overlappendeGyldigMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2),
                LocalDate.EPOCH.minusYears(1)))
        ).isFalse();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3),
                LocalDate.EPOCH.plusYears(5L)))).isFalse();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1),
                LocalDate.EPOCH.plusYears(1)))).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_5() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_6() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_7() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            lagMedlemskapsDokument(), lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), null))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_overlappendePeriodeIkkeGyldigPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            medlemskapDokument, lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        ).isFalse();
    }

    @Test
    void overlappendeMedlemsperiodeIkkeAvvist_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeIkkeAvvistMedlemsperiode(
            medlemskapDokument, new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        ).isTrue();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            medlemskapDokument, lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        ).isFalse();
    }

    @Test
    void overlappendeGyldigMedlemsperiode_lovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        lovvalgsperiode.setMedlPeriodeID(123L);
        assertThat(OverlappendeMedlemskapsperioderKontroller.harOverlappendeGyldigMedlemsperiode(
            medlemskapDokument, lovvalgsperiode)
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

    private static Lovvalgsperiode lagLovvalgsPeriode(LocalDate fom, LocalDate tom) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);
        return lovvalgsperiode;
    }
}
