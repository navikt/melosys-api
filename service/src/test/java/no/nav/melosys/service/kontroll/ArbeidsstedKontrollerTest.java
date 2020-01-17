package no.nav.melosys.service.kontroll;

import java.util.List;

import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.Adresse;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeidsstedKontrollerTest {
    @Test
    void arbeidstedSvalbardOgJanMayen_landErSJ_true() {
        assertThat(ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(lagSedDokument("SJ", "by"))).isTrue();
    }

    @Test
    void arbeidstedSvalbardOgJanMayen_landErIkkeSJ_false() {
        assertThat(ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(lagSedDokument("JS", "by"))).isFalse();
    }

    @Test
    void arbeidstedSvalbardOgJanMayen_byFraSvalbard_true() {
        assertThat(ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(lagSedDokument("JS", "Et sted, nær Ny-Ålesund"))).isTrue();
    }

    @Test
    void arbeidstedSvalbardOgJanMayen_byIkkeFraSvalbard_false() {
        assertThat(ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(lagSedDokument("JS", "New-Holesound"))).isFalse();
    }

    private SedDokument lagSedDokument(String landKode, String  by) {
        SedDokument sedDokument = new SedDokument();
        Adresse adresse_1 = new Adresse();
        adresse_1.by = "By_1";
        adresse_1.land = "XY";
        Adresse adresse_2 = new Adresse();
        adresse_2.by = by;
        adresse_2.land = landKode;
        Arbeidssted arbeidssted_1 = new Arbeidssted("sted1", adresse_1);
        Arbeidssted arbeidssted_2 = new Arbeidssted("sted2", adresse_2);
        List<Arbeidssted> arbeidssteder = List.of(arbeidssted_1, arbeidssted_2);
        sedDokument.setArbeidssteder(arbeidssteder);
        return sedDokument;
    }
}