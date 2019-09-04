package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnntaksperiodeKontrollerTest {


    @Test
    public void feilIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.feilIPeriode(kontrollData(null, null))).isEqualTo(Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    public void periodeErÅpen_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.periodeErÅpen(kontrollData(LocalDate.now(), null))).isEqualTo(Unntak_periode_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    public void periodeOver24Mnd_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.periodeOver24Mnd(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    public void periodeEldreEnn5År_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.periodeEldreEnn3År(kontrollData(LocalDate.now().minusYears(10), null))).isEqualTo(Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL);
    }

    @Test
    public void periodeOver1ÅrFremITid_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.periodeOver1ÅrFremITid(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID);
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    public void utbetaltBarnetrygdytelserIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.utbetaltBarnetrygdytelserIPeriode(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    public void lovvalgslandErNorge_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.lovvalgslandErNorge(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE);
    }

    @Test
    public void overlappendeMedlemsperiode_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.overlappendeMedlemsperiode(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    public void statsborgerskapIkkeMedlemsland_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.statsborgerskapIkkeMedlemsland(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
    }

    @Test
    public void personDød_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.personDød(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.PERSON_DOD);
    }

    @Test
    public void personBosattINorge_erFeil_verifiserBegrunnelse() {
        assertThat(UnntaksperiodeKontroller.personBosattINorge(kontrollData())).isEqualTo(Unntak_periode_begrunnelser.BOSATT_I_NORGE);
    }

    private KontrollData kontrollData() {
        return kontrollData(LocalDate.now().plusMonths(15), LocalDate.now().plusYears(10));
    }

    private KontrollData kontrollData(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(fom, tom));
        sedDokument.setLovvalgslandKode(Landkoder.NO);
        sedDokument.getStatsborgerskapKoder().add("US");

        PersonDokument personDokument = new PersonDokument();
        personDokument.dødsdato = LocalDate.now();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land("NOR"));

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        InntektDokument inntektDokument = new InntektDokument();
        Inntekt inntekt = new YtelseFraOffentlige();
        inntekt.utbetaltIPeriode = YearMonth.now().plusYears(2);

        ArbeidsInntektMaaned arbeidsInntektMaaned = new ArbeidsInntektMaaned();
        arbeidsInntektMaaned.arbeidsInntektInformasjon = new ArbeidsInntektInformasjon();
        arbeidsInntektMaaned.getArbeidsInntektInformasjon().getInntektListe().add(inntekt);
        inntektDokument.getArbeidsInntektMaanedListe().add(arbeidsInntektMaaned);

        UtbetalingDokument utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = Collections.singletonList(new Utbetaling());

        return new KontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument, utbetalingDokument);
    }

}