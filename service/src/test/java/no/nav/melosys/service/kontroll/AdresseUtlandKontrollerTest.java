package no.nav.melosys.service.kontroll;

import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ArbeidUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.kontroll.AdresseUtlandKontroller.*;
import static org.assertj.core.api.Assertions.assertThat;

public class AdresseUtlandKontrollerTest {

    private BehandlingsgrunnlagData behandlingsgrunnlagData;

    @Before
    public void setup() {
        behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.arbeidUtland = List.of(new ArbeidUtland());
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());
    }

    @Test
    public void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = AdresseUtlandKontroller.arbeidsstedManglerFelter(behandlingsgrunnlagData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(ARBEID_UTLAND_NAVN, 0), String.format(ARBEID_UTLAND_LAND, 0)));
    }

    @Test
    public void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        Kontrollfeil kontrollfeil = AdresseUtlandKontroller.foretakUtlandManglerFelter(behandlingsgrunnlagData);
        assertThat(kontrollfeil)
            .extracting(Kontrollfeil::getKode, Kontrollfeil::getFelter)
            .contains(List.of(String.format(FORETAK_UTLAND_NAVN, 0), String.format(FORETAK_UTLAND_LAND, 0)));
    }
}
