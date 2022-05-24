package no.nav.melosys.service.ufm.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.eessi.melding.Adresse;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UfmKontrollerTest {
    @Test
    void feilIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.feilIPeriode(kontrollData(null, null))).isEqualTo(Kontroll_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    void periodeErÅpen_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.periodeErÅpen(kontrollData(LocalDate.now(), null))).isEqualTo(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void periodeOver24Mnd_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.periodeOver24Mnd(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void periodeOver5År_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.periodeOver5År(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_5_AR);
    }

    @Test
    void periodeEldreEnn5År_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.periodeStarterFørFørsteJuni2012(kontrollData(LocalDate.now().minusYears(10), null))).isEqualTo(Kontroll_begrunnelser.PERIODE_FOR_GAMMEL);
    }

    @Test
    void periodeOver1ÅrFremITid_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.periodeOver1ÅrFremITid(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID);
    }

    @Test
    void utbetaltYtelserFraOffentligIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData())).isEqualTo(Kontroll_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    void utbetaltBarnetrygdytelser_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.utbetaltBarnetrygdytelser(kontrollData())).isEqualTo(Kontroll_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    void lovvalgslandErNorge_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.lovvalgslandErNorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.LOVVALGSLAND_NORGE);
    }

    @Test
    void overlappendeMedlemsperiode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.overlappendeMedlemsperiode(kontrollData())).isEqualTo(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    void statsborgerskapIkkeMedlemsland_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.statsborgerskapIkkeMedlemsland(kontrollData())).isEqualTo(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
    }

    @Test
    void statsborgerskapStatsløs_erOK_ikkeSjekkMedlemsland() {
        UfmKontrollData kontrollData = kontrollData();
        kontrollData.getSedDokument().setStatsborgerskapKoder(List.of("XS"));
        assertThat(UfmKontroller.statsborgerskapIkkeMedlemsland(kontrollData)).isNull();
    }

    @Test
    void avtalelandErSverige_erOK_ikkeSjekkMedlemsland() {
        UfmKontrollData kontrollData = kontrollData();
        kontrollData.getSedDokument().setAvsenderLandkode(Landkoder.SE);
        assertThat(UfmKontroller.statsborgerskapIkkeMedlemsland(kontrollData)).isNull();
    }

    @Test
    void personDød_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.personDød(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERSON_DOD);
    }

    @Test
    void personBosattINorge_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroller.personBosattINorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.BOSATT_I_NORGE);
    }

    @Test
    void arbeidssted_erSvalbard_verifiserBegrunnelse() {
        assertThat(UfmKontroller.arbeidssted(kontrollData())).isEqualTo(Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS);
    }

    @Test
    void lovvalgslandErNorge_erNorge_registrerTreff() {
        assertThat(UfmKontroller.lovvalgslandErNorge(Landkoder.NO)).isTrue();
    }

    @Test
    void lovvalgslandErNorge_erSverige_ingenTreff() {
        assertThat(UfmKontroller.lovvalgslandErNorge(Landkoder.SE)).isFalse();
    }


    @Test
    void statsborgerskapErMedlemsland_statsborgerSE_registrerTreff() {
        assertThat(UfmKontroller.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode())))
            .isTrue();
    }

    @Test
    void statsborgerskapErMedlemsland_statsborgerSEOgUS_registrerTreff() {
        assertThat(UfmKontroller.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode(), "US")))
            .isTrue();
    }

    @Test
    void statsborgerskapErMedlemsland_statsborgerUS_ingenTreff() {
        assertThat(UfmKontroller.statsborgerskapErMedlemsland(Lists.newArrayList("US")))
            .isFalse();
    }

    private UfmKontrollData kontrollData() {
        return kontrollData(LocalDate.now().plusMonths(15), LocalDate.now().plusYears(10));
    }

    private UfmKontrollData kontrollData(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(fom, tom));
        sedDokument.setLovvalgslandKode(Landkoder.NO);
        sedDokument.getStatsborgerskapKoder().add("US");
        Adresse adresse_1 = new Adresse();
        adresse_1.by = "By_1";
        adresse_1.land = "XY";
        Adresse adresse_2 = new Adresse();
        adresse_2.by = "By_2";
        adresse_2.land = "SJ";
        Arbeidssted arbeidssted_1 = new Arbeidssted("sted1", adresse_1);
        Arbeidssted arbeidssted_2 = new Arbeidssted("sted2", adresse_2);
        List<Arbeidssted> arbeidssteder = List.of(arbeidssted_1, arbeidssted_2);
        sedDokument.setArbeidssteder(arbeidssteder);

        PersonDokument personDokument = new PersonDokument();
        personDokument.setDødsdato(LocalDate.now());
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land("NOR"));

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
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

        return new UfmKontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument, utbetalingDokument);
    }
}
