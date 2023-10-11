package no.nav.melosys.service.kontroll.feature.ufm.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UfmKontrollTest {

    private final LocalDate DATE = LocalDate.EPOCH;

    @Test
    void feilIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.feilIPeriode(kontrollData(null, null))).isEqualTo(Kontroll_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    void periodeErÅpen_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.periodeErÅpen(kontrollData(DATE, null))).isEqualTo(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void periodeOver24MndOgEnDag_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void periodeOver24MndOgEnDag_medNøyaktig2ÅrOg1Dag_erRett() {
        UfmKontrollData kontrollData = kontrollData(DATE.plusYears(2), DATE.plusYears(4));


        assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData)).isNull();
    }

    @Test
    void periodeOver24MndOgEnDag_medOver1DagOverlapp_erFeil_verifiserBegrunnelse() {
        UfmKontrollData kontrollData = kontrollData(DATE.plusYears(2).minusDays(1), DATE.plusYears(4));


        assertThat(UfmKontroll.periodeOver24MånederOgEnDag(kontrollData)).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }


    @Test
    void periodeOver5År_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.periodeOver5År(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERIODEN_OVER_5_AR);
    }

    @Test
    void periodeEldreEnn5År_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.periodeStarterFørFørsteJuni2012(kontrollData(DATE.minusYears(11), null))).isEqualTo(Kontroll_begrunnelser.PERIODE_FOR_GAMMEL);
    }

    @Test
    void periodeOver1ÅrFremITid_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.periodeOver1ÅrFremITid(kontrollData(LocalDate.now()))).isEqualTo(Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID);
    }

    @Test
    void utbetaltYtelserFraOffentligIPeriode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.utbetaltYtelserFraOffentligIPeriode(kontrollData(LocalDate.now()))).isEqualTo(Kontroll_begrunnelser.MOTTAR_YTELSER);
    }

    @Test
    void lovvalgslandErNorge_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.lovvalgslandErNorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.LOVVALGSLAND_NORGE);
    }

    @Test
    void overlappendeMedlemsperiode_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.overlappendeMedlemsperiode(kontrollData())).isEqualTo(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER);
    }

    @Test
    void statsborgerskapIkkeMedlemsland_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData())).isEqualTo(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
    }

    @Test
    void statsborgerskapStatsløs_erOK_ikkeSjekkMedlemsland() {
        UfmKontrollData kontrollData = kontrollData();
        kontrollData.sedDokument().setStatsborgerskapKoder(List.of("XS"));
        assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData)).isNull();
    }

    @Test
    void avtalelandErSverige_erOK_ikkeSjekkMedlemsland() {
        UfmKontrollData kontrollData = kontrollData();
        kontrollData.sedDokument().setAvsenderLandkode(Landkoder.SE);
        assertThat(UfmKontroll.statsborgerskapIkkeMedlemsland(kontrollData)).isNull();
    }

    @Test
    void personDød_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.personDød(kontrollData())).isEqualTo(Kontroll_begrunnelser.PERSON_DOD);
    }

    @Test
    void personBosattINorge_erFeil_verifiserBegrunnelse() {
        assertThat(UfmKontroll.personBosattINorge(kontrollData())).isEqualTo(Kontroll_begrunnelser.BOSATT_I_NORGE);
    }

    @Test
    void arbeidssted_erSvalbard_verifiserBegrunnelse() {
        assertThat(UfmKontroll.arbeidssted(kontrollData())).isEqualTo(Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS);
    }

    private UfmKontrollData kontrollData() {
        return kontrollData(DATE.plusMonths(15), DATE.plusYears(10));
    }

    private UfmKontrollData kontrollData(LocalDate localDate) {
        return kontrollData(localDate.plusMonths(15), localDate.plusYears(10));
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
        personDokument.setDødsdato(DATE);
        personDokument.setBostedsadresse(new Bostedsadresse());
        personDokument.getBostedsadresse().setLand(new Land("NOR"));

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(DATE, DATE.plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        InntektDokument inntektDokument = new InntektDokument();
        Inntekt inntekt = new YtelseFraOffentlige();
        inntekt.utbetaltIPeriode = YearMonth.now().plusYears(2);

        ArbeidsInntektMaaned arbeidsInntektMaaned = new ArbeidsInntektMaaned(null, null, new ArbeidsInntektInformasjon());
        arbeidsInntektMaaned.arbeidsInntektInformasjon.getInntektListe().add(inntekt);
        List<ArbeidsInntektMaaned> arbeidsInntektMaanedListe = new ArrayList<>();
        arbeidsInntektMaanedListe.add(arbeidsInntektMaaned);
        inntektDokument.setArbeidsInntektMaanedListe(arbeidsInntektMaanedListe);

        UtbetalingDokument utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = Collections.singletonList(new Utbetaling());

        return new UfmKontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument,
            utbetalingDokument, null);
    }
}
