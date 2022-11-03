package no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll;

import java.util.List;

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeidUtlandKontrollTest {

    private MottatteOpplysningerData mottatteOpplysningerData;

    @BeforeEach
    void setup() {
        mottatteOpplysningerData = new MottatteOpplysningerData();
        final var fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.virksomhetNavn = " ";
        mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder = List.of(fysiskArbeidssted);
        mottatteOpplysningerData.foretakUtland = List.of(new ForetakUtland());
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = ArbeidUtlandKontroll.arbeidsstedManglerFelter(mottatteOpplysningerData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(ArbeidUtlandKontroll.ARBEIDSSTED_FIRMANAVN, 0), String.format(ArbeidUtlandKontroll.ARBEIDSSTED_LAND, 0)));
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = ArbeidUtlandKontroll.foretakUtlandManglerFelter(mottatteOpplysningerData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(ArbeidUtlandKontroll.FORETAK_UTLAND_NAVN, 0), String.format(ArbeidUtlandKontroll.FORETAK_UTLAND_LAND, 0)));
    }
}
