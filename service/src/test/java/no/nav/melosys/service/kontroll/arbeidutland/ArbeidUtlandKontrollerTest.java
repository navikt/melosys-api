package no.nav.melosys.service.kontroll.arbeidutland;

import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeidUtlandKontrollerTest {

    private BehandlingsgrunnlagData behandlingsgrunnlagData;

    @BeforeEach
    void setup() {
        behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        final var fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.virksomhetNavn = " ";
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(fysiskArbeidssted);
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = ArbeidUtlandKontroller.arbeidsstedManglerFelter(behandlingsgrunnlagData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(ArbeidUtlandKontroller.ARBEIDSSTED_FIRMANAVN, 0), String.format(ArbeidUtlandKontroller.ARBEIDSSTED_LAND, 0)));
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = ArbeidUtlandKontroller.foretakUtlandManglerFelter(behandlingsgrunnlagData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(ArbeidUtlandKontroller.FORETAK_UTLAND_NAVN, 0), String.format(ArbeidUtlandKontroller.FORETAK_UTLAND_LAND, 0)));
    }
}
