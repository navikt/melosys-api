package no.nav.melosys.service.kontroll.regler;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UfmReglerTest {

    @Test
    void statsborgerskapErMedlemsland_statsborgerSE_registrerTreff() {
        assertThat(UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode())))
            .isTrue();
    }

    @Test
    void statsborgerskapErMedlemsland_statsborgerSEOgUS_registrerTreff() {
        assertThat(UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.getKode(), "US")))
            .isTrue();
    }

    @Test
    void statsborgerskapErMedlemsland_statsborgerUS_ingenTreff() {
        assertThat(UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList("US")))
            .isFalse();
    }

    @Test
    void lovvalgslandErNorge_erNorge_registrerTreff() {
        assertThat(UfmRegler.lovvalgslandErNorge(Landkoder.NO)).isTrue();
    }

    @Test
    void lovvalgslandErNorge_erSverige_ingenTreff() {
        assertThat(UfmRegler.lovvalgslandErNorge(Landkoder.SE)).isFalse();
    }
}
