package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BehandlingsgrunnlagUtilsTest {

    @Test
    public void hentSoeknadsland() {
        Soeknad soeknad = new Soeknad();
        soeknad.soeknadsland.landkoder = Arrays.asList(Landkoder.BE.getKode(), Landkoder.BG.getKode());

        List<String> strings = BehandlingsgrunnlagUtils.hentSøknadsland(soeknad);
        assertThat(strings).contains(Landkoder.BE.getKode(), Landkoder.BG.getKode());
    }

    @Test
    public void hentOppgittAdresse_medGatenavnOgLand_ErIkkeNull() {
        Soeknad søknad = new Soeknad();
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.gatenavn = "HjemGata";
        oppgittAdresse.landkode = "NO";
        søknad.bosted.oppgittAdresse = oppgittAdresse;
        assertThat(BehandlingsgrunnlagUtils.hentBostedsadresse(søknad)).isNotNull();
    }

    @Test
    public void hentOppgittAdresse_somErTom_ErNull() {
        Soeknad søknad = new Soeknad();
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        assertThat(BehandlingsgrunnlagUtils.hentBostedsadresse(søknad)).isNull();
    }

    @Test
    public void hentPeriode_opphold() {
        Soeknad soeknad = new Soeknad();
        leggTilFysiskArbeidssted(soeknad);

        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.periode = periode_2;

        Periode res = BehandlingsgrunnlagUtils.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_2);
    }

    @Test
    public void hentOppgittBostedsland_landkodeSverige_girLandkode() {
        Soeknad soeknad = new Soeknad();
        soeknad.bosted.oppgittAdresse.landkode = "SE";

        Optional<Landkoder> landkoder = BehandlingsgrunnlagUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isPresent()
            .contains(Landkoder.SE);
    }

    @Test
    public void hentOppgittBostedsland_eksistererIkke_girEmpty() {
        Soeknad soeknad = new Soeknad();

        Optional<Landkoder> landkoder = BehandlingsgrunnlagUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder).isEmpty();
    }

    private void leggTilFysiskArbeidssted(Soeknad soeknad) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse = new StrukturertAdresse();
        fysiskArbeidssted.adresse.landkode = Landkoder.BE.getKode();
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = Collections.singletonList(fysiskArbeidssted);
    }
}
