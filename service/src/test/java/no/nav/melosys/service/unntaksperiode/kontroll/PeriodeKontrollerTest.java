package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodeKontrollerTest {

    @Test
    public void gyldigPeriode_erGyldigPeriode_ingenregistrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(2));
        assertThat(PeriodeKontroller.gyldigPeriode(kontrollData)).isNull();
    }

    @Test
    public void gyldigPeriode_tomFørFom_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().plusYears(1), LocalDate.now());
        assertThat(PeriodeKontroller.gyldigPeriode(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    public void periodeErÅpen_ingenTilDato_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().plusMonths(6), null);
        assertThat(PeriodeKontroller.periodeErÅpen(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.INGEN_SLUTTDATO);
    }
    
    @Test
    public void periodeErÅpen_ikkeÅpen_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(1));
        assertThat(PeriodeKontroller.periodeErÅpen(kontrollData)).isNull();
    }

    @Test
    public void periodeMaks24Mnd_periodeOver24Mnd_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now(), LocalDate.now().plusYears(3));
        assertThat(PeriodeKontroller.periodeMaks24Mnd(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD);
    }
    
    @Test
    public void periodeMaks24Mnd_periode14Mnd_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now(), LocalDate.now().plusMonths(14));
        assertThat(PeriodeKontroller.periodeMaks24Mnd(kontrollData)).isNull();
    }

    @Test
    public void periodeEldreEnn5År_periodeEldreEnn5År_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().minusYears(6), LocalDate.now().plusYears(3));
        assertThat(PeriodeKontroller.periodeEldreEnn5År(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL);
    }
    
    @Test
    public void periodeEldreEnn5År_periodeFraNå_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now(), LocalDate.now().plusYears(3));
        assertThat(PeriodeKontroller.periodeEldreEnn5År(kontrollData)).isNull();
    }
    
    @Test
    public void periodeOver1ÅrFremITid_periodeOm2År_registrerTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now().plusYears(2), LocalDate.now().plusYears(3));
        assertThat(PeriodeKontroller.periodeOver1ÅrFremITid(kontrollData)).isEqualTo(Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID);
    }

    @Test
    public void periodeOver1ÅrFremITid_periodeNå_ingenTreff() {
        KontrollData kontrollData = hentKontrollData(LocalDate.now(), LocalDate.now().plusYears(3));
        assertThat(PeriodeKontroller.periodeOver1ÅrFremITid(kontrollData)).isNull();
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;
    }

    private KontrollData hentKontrollData(LocalDate fom, LocalDate tom) {
        return new KontrollData(hentSedDokument(fom, tom), null, null, null);
    }
}