package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MottatteOpplysningerUtilsTest {

    @Test
    public void hentSoeknadsland() {
        Soeknad soeknad = new Soeknad();
        soeknad.soeknadsland.landkoder = Arrays.asList(Landkoder.BE.getKode(), Landkoder.BG.getKode());
        soeknad.soeknadsland.erUkjenteEllerAlleEosLand = true;

        Soeknadsland soeknadsland = MottatteOpplysningerUtils.hentSøknadsland(soeknad);
        assertThat(soeknadsland.landkoder).contains(Landkoder.BE.getKode(), Landkoder.BG.getKode());
        assertThat(soeknadsland.erUkjenteEllerAlleEosLand).isTrue();
    }

    @Test
    public void hentOppgittAdresse_medGatenavnOgLand_ErIkkeNull() {
        Soeknad søknad = new Soeknad();
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.setGatenavn("HjemGata");
        oppgittAdresse.setLandkode("NO");
        søknad.bosted.oppgittAdresse = oppgittAdresse;
        assertThat(MottatteOpplysningerUtils.hentBostedsadresse(søknad)).isNotNull();
    }

    @Test
    public void hentOppgittAdresse_somErTom_ErNull() {
        Soeknad søknad = new Soeknad();
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        assertThat(MottatteOpplysningerUtils.hentBostedsadresse(søknad)).isNull();
    }

    @Test
    public void hentPeriode_opphold() {
        Soeknad soeknad = new Soeknad();
        leggTilFysiskArbeidssted(soeknad);

        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.periode = periode_2;

        Periode res = MottatteOpplysningerUtils.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_2);
    }

    @Test
    public void hentOppgittBostedsland_landkodeSverige_girLandkode() {
        Soeknad soeknad = new Soeknad();
        soeknad.bosted.oppgittAdresse.setLandkode("SE");

        Optional<Bostedsland> landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isPresent()
            .contains(new Bostedsland(Landkoder.SE));
    }

    @Test
    public void hentOppgittBostedsland_eksistererIkke_girEmpty() {
        Soeknad soeknad = new Soeknad();

        Optional<Bostedsland> landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isEmpty();
    }

    private void leggTilFysiskArbeidssted(Soeknad soeknad) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse = new StrukturertAdresse();
        fysiskArbeidssted.adresse.setLandkode(Landkoder.BE.getKode());
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = Collections.singletonList(fysiskArbeidssted);
    }
}
