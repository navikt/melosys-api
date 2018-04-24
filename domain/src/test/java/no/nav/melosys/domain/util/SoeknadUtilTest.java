package no.nav.melosys.domain.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.OppholdUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SoeknadUtilTest {

    @Test
    public void hentLand_arbeidUtland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.arbeidUtland.arbeidsland = Arrays.asList(new Land(Land.BELGIA), new Land(Land.BULGARIA));

        List<String> strings = SoeknadUtil.hentLand(soeknad);
        assertThat(strings).contains(Land.BELGIA, Land.BULGARIA);
    }

    @Test
    public void hentLand_oppholdUtland() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.oppholdUtland.oppholdsland = Arrays.asList(new Land(Land.BELGIA), new Land(Land.BULGARIA));

        List<String> strings = SoeknadUtil.hentLand(soeknad);
        assertThat(strings).contains(Land.BELGIA, Land.BULGARIA);
    }

    @Test
    public void hentPeriode_arbeid() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.arbeidUtland = new ArbeidUtland();
        soeknad.oppholdUtland = new OppholdUtland();

        Periode periode_1 = new Periode(LocalDate.MIN, LocalDate.MAX);
        soeknad.arbeidUtland.arbeidsperiode = periode_1;
        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.oppholdUtland.oppholdsPeriode = periode_2;

        Periode res = SoeknadUtil.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_1);
    }

    @Test
    public void hentPeriode_opphold() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.arbeidUtland = new ArbeidUtland();
        soeknad.oppholdUtland = new OppholdUtland();

        soeknad.arbeidUtland.arbeidsperiode = null;
        Periode periode_2 = new Periode(LocalDate.MIN.plusYears(1), LocalDate.MAX);
        soeknad.oppholdUtland.oppholdsPeriode = periode_2;

        Periode res = SoeknadUtil.hentPeriode(soeknad);
        assertThat(res).isEqualTo(periode_2);
    }

    @Test(expected = RuntimeException.class)
    public void hentPeriode_ingen() {
        SoeknadDokument soeknad = new SoeknadDokument();
        soeknad.arbeidUtland = new ArbeidUtland();
        soeknad.oppholdUtland = new OppholdUtland();

        SoeknadUtil.hentPeriode(soeknad);
    }
}