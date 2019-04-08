package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.*;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SoeknadUtilsTest {

    @Test
    public void hentLand_arbeidUtland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        leggTilArbeidUtland(soeknad);

        List<String> strings = SoeknadUtils.hentLand(soeknad);
        assertThat(strings).contains(Land.BELGIA);
    }

    @Test
    public void hentLand_oppholdUtland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.oppholdUtland = new OppholdUtland();
        soeknad.oppholdUtland.oppholdslandkoder = Arrays.asList(new Land(Land.BELGIA).getKode(), new Land(Land.BULGARIA).getKode());

        List<String> strings = SoeknadUtils.hentLand(soeknad);
        assertThat(strings).contains(Land.BELGIA, Land.BULGARIA);
    }

    @Test
    public void hentLand_soeknadsland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.soeknadsland.landkoder = Arrays.asList(new Land(Land.BELGIA).getKode(), new Land(Land.BULGARIA).getKode());

        List<String> strings = SoeknadUtils.hentLand(soeknad);
        assertThat(strings).contains(Land.BELGIA, Land.BULGARIA);
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

    private void leggTilArbeidUtland(SoeknadDokument soeknad) {
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landkode = new Land(Land.BELGIA).getKode();
        soeknad.arbeidUtland = Collections.singletonList(arbeidUtland);
    }
}