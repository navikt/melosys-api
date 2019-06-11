package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MedlemskapKontrollerTest {
    
    @Test
    public void lovvalgslandErNorge_erNorge_registrerTreff() {
        SedDokument sedDokument = hentSedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.NO);
        assertThat(MedlemskapKontroller.lovvalgslandErNorge(hentKontrollData(sedDokument, null)))
            .isEqualTo(Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE);
    }

    @Test
    public void lovvalgslandErNorge_erSverige_ingenTreff() {
        SedDokument sedDokument = hentSedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.SE);
        assertThat(MedlemskapKontroller.lovvalgslandErNorge(hentKontrollData(sedDokument, null)))
            .isNull();
    }

    @Test
    public void overlappendeMedlemsperiode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isNull();
    }

    @Test
    public void overlappendeMedlemsperiode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().plusYears(3), LocalDate.now().plusYears(5L));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isNull();
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_1() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now(), LocalDate.now().plusYears(1));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_2() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().plusYears(1), LocalDate.now().plusYears(5));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_3() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().minusYears(1), LocalDate.now().plusYears(5));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriode_registrerTreff_4() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void overlappendeMedlemsperiode_overlappendePeriodeOgTomErNull_registrerTreff() {
        SedDokument sedDokument = hentSedDokument(LocalDate.now().minusYears(1), null);
        assertThat(MedlemskapKontroller.overlappendeMedlemsperiode(hentKontrollData(sedDokument, hentMedlemskapsDokument())))
            .isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerSE_ingenTreff() {
        SedDokument sedDokument = hentSedDokument();
        sedDokument.getStatsborgerskapKoder().add(Landkoder.SE.getKode());
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(hentKontrollData(sedDokument, null)))
            .isNull();
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerSEOgUS_ingenTreff() {
        SedDokument sedDokument = hentSedDokument();
        sedDokument.getStatsborgerskapKoder().add(Landkoder.SE.getKode());
        sedDokument.getStatsborgerskapKoder().add("US");
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(hentKontrollData(sedDokument, null)))
            .isNull();
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_statsborgerUS_registrerTreff() {
        SedDokument sedDokument = hentSedDokument();
        sedDokument.getStatsborgerskapKoder().add("US");
        assertThat(MedlemskapKontroller.statsborgerskapIkkeMedlemsland(hentKontrollData(sedDokument, null)))
            .isEqualTo(Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
    }

    private MedlemskapDokument hentMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }

    private SedDokument hentSedDokument() {
        return hentSedDokument(LocalDate.now(), LocalDate.now());
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;
    }

    private KontrollData hentKontrollData(SedDokument sedDokument, MedlemskapDokument medlemskapDokument) {
        return new KontrollData(sedDokument, null, medlemskapDokument, null);
    }
}