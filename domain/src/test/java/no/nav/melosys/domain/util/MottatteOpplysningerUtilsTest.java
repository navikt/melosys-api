package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MottatteOpplysningerUtilsTest {

    @Test
    void hentSoeknadsland() {
        Soeknad soeknad = new Soeknad();
        soeknad.soeknadsland.setLandkoder(Arrays.asList(Landkoder.BE.getKode(), Landkoder.BG.getKode()));
        soeknad.soeknadsland.setErUkjenteEllerAlleEosLand(true);

        Soeknadsland soeknadsland = MottatteOpplysningerUtils.hentSøknadsland(soeknad);
        assertThat(soeknadsland.getLandkoder()).contains(Landkoder.BE.getKode(), Landkoder.BG.getKode());
        assertThat(soeknadsland.isErUkjenteEllerAlleEosLand()).isTrue();
    }

    @Test
    void hentOppgittAdresse_medGatenavnOgLand_ErIkkeNull() {
        Soeknad søknad = new Soeknad();
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.setGatenavn("HjemGata");
        oppgittAdresse.setLandkode("NO");
        søknad.bosted.setOppgittAdresse(oppgittAdresse);
        assertThat(MottatteOpplysningerUtils.hentBostedsadresse(søknad)).isNotNull();
    }

    @Test
    void hentOppgittAdresse_somErTom_ErNull() {
        Soeknad søknad = new Soeknad();
        søknad.bosted.setOppgittAdresse(new StrukturertAdresse());
        assertThat(MottatteOpplysningerUtils.hentBostedsadresse(søknad)).isNull();
    }

    @Test
    void hentPeriode_opphold() {
        Soeknad soeknad = new Soeknad();
        leggTilFysiskArbeidssted(soeknad);

        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.periode = periode_2;

        Periode res = MottatteOpplysningerUtils.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_2);
    }

    @Test
    void hentOppgittBostedsland_landkodeSverige_girLandkode() {
        Soeknad soeknad = new Soeknad();
        soeknad.bosted.getOppgittAdresse().setLandkode("SE");

        Optional<Bostedsland> landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isPresent()
            .contains(new Bostedsland(Landkoder.SE));
    }

    @Test
    void hentOppgittBostedsland_eksistererIkke_girEmpty() {
        Soeknad soeknad = new Soeknad();

        Optional<Bostedsland> landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isEmpty();
    }

    private void leggTilFysiskArbeidssted(Soeknad soeknad) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.setAdresse(new StrukturertAdresse());
        fysiskArbeidssted.getAdresse().setLandkode(Landkoder.BE.getKode());
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(Collections.singletonList(fysiskArbeidssted));
    }
}
