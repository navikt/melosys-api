package no.nav.melosys.tjenester.gui;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdresseTjenesteTest {
    private AdresseTjeneste adresseTjeneste;

    @BeforeEach
    void setUp() throws IkkeFunnetException {
        UtenlandskMyndighetService utenlandskMyndighetRepo = mock(UtenlandskMyndighetService.class);
        adresseTjeneste = new AdresseTjeneste(utenlandskMyndighetRepo);

        UtenlandskMyndighet danmark = new UtenlandskMyndighet();
        danmark.land = "Denmark";
        danmark.landkode = Landkoder.DK;

        UtenlandskMyndighet sverige = new UtenlandskMyndighet();
        sverige.land = "Sweden";
        sverige.landkode = Landkoder.SE;

        when(utenlandskMyndighetRepo.hentUtenlandskMyndighet(Landkoder.DK)).thenReturn(danmark);
        when(utenlandskMyndighetRepo.hentUtenlandskMyndighet(Landkoder.SE)).thenReturn(sverige);
        when(utenlandskMyndighetRepo.hentAlleUtenlandskMyndigheter()).thenReturn(Arrays.asList(sverige, danmark));
    }

    @Test
    public void hentMyndighet_gyldigLandkode() throws FunksjonellException {
        ResponseEntity response = adresseTjeneste.hentMyndighet(Landkoder.DK);
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(UtenlandskMyndighet.class);

        UtenlandskMyndighet myndighet = (UtenlandskMyndighet) response.getBody();
        assertThat(myndighet.landkode).isEqualTo(Landkoder.DK);
        assertThat(myndighet.land).isEqualTo("Denmark");
    }

    @Test
    public void hentMyndighet_ikkeGyldigLandkode() throws FunksjonellException {
        ResponseEntity response = adresseTjeneste.hentMyndighet(Landkoder.NO);
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void hentMyndigheter() {
        ResponseEntity response = adresseTjeneste.hentMyndigheter();
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(List.class);

        List myndigheter = (List) response.getBody();
        assertThat(myndigheter.size()).isEqualTo(2);
        assertThat(myndigheter.get(0)).isInstanceOf(UtenlandskMyndighet.class);
    }
}
