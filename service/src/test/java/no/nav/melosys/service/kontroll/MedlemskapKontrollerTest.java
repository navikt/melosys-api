package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.Collections;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MedlemskapKontrollerTest {

    @Test
    public void lovvalgslandErNorge_erNorge_registrerTreff() {
        assertThat(MedlemskapKontroller.lovvalgslandErNorge(Landkoder.NO)).isTrue();
    }

    @Test
    public void lovvalgslandErNorge_erSverige_ingenTreff() {
        assertThat(MedlemskapKontroller.lovvalgslandErNorge(Landkoder.SE)).isFalse();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().minusYears(2), LocalDate.now().minusYears(1), hentMedlemskapsDokument())
        ).isFalse();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().plusYears(3), LocalDate.now().plusYears(5L), hentMedlemskapsDokument()
        )).isFalse();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now(), LocalDate.now().plusYears(1), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().plusYears(1), LocalDate.now().plusYears(5), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(5), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), hentMedlemskapsDokument())).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_5() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now(), LocalDate.now().plusYears(2), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_6() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().plusYears(2), LocalDate.now().plusYears(3), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriode_registrerTreff_7() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().plusYears(2), LocalDate.now().plusYears(2), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now().minusYears(1), null, hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_overlappendePeriodeIkkeGyldigPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = hentMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now(), LocalDate.now().plusYears(2), medlemskapDokument)
        ).isFalse();
    }

    @Test
    public void overlappendeMedlemsperiodeIkkeAvvist_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = hentMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeIkkeAvvistPeriode(
            LocalDate.now(), LocalDate.now().plusYears(2), medlemskapDokument)
        ).isTrue();
    }

    @Test
    public void statsborgerskapErMedlemsland_statsborgerSE_registrerTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode())))
            .isTrue();
    }

    @Test
    public void statsborgerskapErMedlemsland_statsborgerSEOgUS_registrerTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode(), "US")))
            .isTrue();
    }

    @Test
    public void statsborgerskapErMedlemsland_statsborgerUS_ingenTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapErMedlemsland(Lists.newArrayList("US")))
            .isFalse();
    }

    @Test
    public void overlappendeGyldigMedlemsperiode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = hentMedlemskapsDokument();
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(
            LocalDate.now(), LocalDate.now().plusYears(2), medlemskapDokument)
        ).isFalse();
    }

    private MedlemskapDokument hentMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }
}
