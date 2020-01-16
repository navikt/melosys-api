package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.Collections;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.Test;

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
    public void overlappendeMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().minusYears(2), LocalDate.now().minusYears(1), hentMedlemskapsDokument())
        ).isFalse();
    }

    @Test
    public void overlappendeMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().plusYears(3), LocalDate.now().plusYears(5L), hentMedlemskapsDokument()
        )).isFalse();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now(), LocalDate.now().plusYears(1), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().plusYears(1), LocalDate.now().plusYears(5), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(5), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), hentMedlemskapsDokument())).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_5() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now(), LocalDate.now().plusYears(2), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_6() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().plusYears(2), LocalDate.now().plusYears(3), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_7() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().plusYears(2), LocalDate.now().plusYears(2), hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(
            LocalDate.now().minusYears(1), null, hentMedlemskapsDokument())
        ).isTrue();
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerSE_ingenTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(Lists.newArrayList(Landkoder.SE.getKode())))
            .isFalse();
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerSEOgUS_ingenTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(Lists.newArrayList(Landkoder.SE.getKode(), "US")))
            .isFalse();
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerUS_registrerTreff() {
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(Lists.newArrayList("US")))
            .isTrue();
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