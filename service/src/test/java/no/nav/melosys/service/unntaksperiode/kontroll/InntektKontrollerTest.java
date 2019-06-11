package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InntektKontrollerTest {

    @Test
    public void utførSteg_finnerIngenTreff_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        SedDokument sedDokument = hentSedDokument(fom ,tom);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(false, fom))))
            .isNull();
    }

    @Test
    public void utførSteg_finnerTreff_nyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        SedDokument sedDokument = hentSedDokument(fom ,tom);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(true, fom.minusMonths(1)))))
            .isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    public void utførSteg_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        SedDokument sedDokument = hentSedDokument(fom ,tom);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(true,  fom.minusYears(4)))))
            .isNull();
    }

    @Test
    public void utførSteg_tomTilDato_forespørTomTilDato() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);

        SedDokument sedDokument = hentSedDokument(fom,null);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(true, fom))))
            .isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    public void utførSteg_periodePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        SedDokument sedDokument = hentSedDokument(fom ,tom);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(true, fom))))
            .isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    public void utførSteg_periodeIkkePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        SedDokument sedDokument = hentSedDokument(fom ,tom);
        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentKontrollData(sedDokument, hentInntektDokument(true, fom))))
            .isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }


    private InntektDokument hentInntektDokument(boolean medYtelserFraOffentlig, LocalDate fom) {
        InntektDokument inntektDokument = new InntektDokument();

        inntektDokument.arbeidsInntektMaanedListe = new ArrayList<>();
        inntektDokument.arbeidsInntektMaanedListe.add(hentArbeidsInntektMaaned(medYtelserFraOffentlig, fom));

        return inntektDokument;
    }

    private ArbeidsInntektMaaned hentArbeidsInntektMaaned(boolean medYtelserFraOffentlig, LocalDate fom) {
        ArbeidsInntektMaaned arbeidsInntektMaaned = new ArbeidsInntektMaaned();
        arbeidsInntektMaaned.arbeidsInntektInformasjon = new ArbeidsInntektInformasjon();
        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe = hentInntektsListe(medYtelserFraOffentlig, fom);

        return arbeidsInntektMaaned;
    }

    private List<Inntekt> hentInntektsListe(boolean medYtelserFraOffentlig, LocalDate fom) {
        List<Inntekt> inntektsListe = new ArrayList<>();
        inntektsListe.add(new Inntekt());

        if (medYtelserFraOffentlig) {
            YtelseFraOffentlige ytelseFraOffentlige = new YtelseFraOffentlige();
            ytelseFraOffentlige.utbetaltIPeriode = YearMonth.from(fom).plusMonths(1);
            inntektsListe.add(ytelseFraOffentlige);
        }

        return inntektsListe;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;

    }

    private KontrollData hentKontrollData(SedDokument sedDokument, InntektDokument inntektDokument) {
        return new KontrollData(sedDokument, null, null, inntektDokument);
    }
}