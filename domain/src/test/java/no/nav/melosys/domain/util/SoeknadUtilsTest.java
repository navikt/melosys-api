package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SoeknadUtilsTest {

    @Test
    public void hentSoeknadsland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.soeknadsland.landkoder = Arrays.asList(Landkoder.BE.getKode(), Landkoder.BG.getKode());

        List<String> strings = SoeknadUtils.hentSøknadsland(soeknad);
        assertThat(strings).contains(Landkoder.BE.getKode(), Landkoder.BG.getKode());
    }

    @Test
    public void hentOppgittAdresse_medGatenavnOgLand_ErIkkeNull() {
        SoeknadDokument søknad = new SoeknadDokument();
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.gatenavn = "HjemGata";
        oppgittAdresse.landkode = "NO";
        søknad.bosted.oppgittAdresse = oppgittAdresse;
        assertThat(SoeknadUtils.hentBostedsadresse(søknad)).isNotNull();
    }

    @Test
    public void hentOppgittAdresse_somErTom_ErNull() {
        SoeknadDokument søknad = new SoeknadDokument();
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        søknad.bosted.oppgittAdresse = oppgittAdresse;
        assertThat(SoeknadUtils.hentBostedsadresse(søknad)).isNull();
    }

    @Test
    public void hentPeriode_opphold() {
        SoeknadDokument soeknad = new SoeknadDokument();
        leggTilArbeidUtland(soeknad);

        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.periode = periode_2;

        Periode res = SoeknadUtils.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_2);
    }

    @Test
    public void hentOppgittBostedsland_landkodeSverige_girLandkode() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.bosted.oppgittAdresse.landkode = "SE";

        Optional<Landkoder> landkoder = SoeknadUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder.get()).isEqualTo(Landkoder.SE);
    }

    @Test
    public void hentOppgittBostedsland_eksistererIkke_girEmpty() {
        SoeknadDokument soeknad = new SoeknadDokument();

        Optional<Landkoder> landkoder = SoeknadUtils.hentOppgittBostedsland(soeknad);
        assertThat(landkoder.isPresent()).isFalse();
    }

    private void leggTilArbeidUtland(SoeknadDokument soeknad) {
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landkode = Landkoder.BE.getKode();
        soeknad.arbeidUtland = Collections.singletonList(arbeidUtland);
    }
}