package no.nav.melosys.tjenester.gui;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdresseTjenesteTest {

    private AdresseTjeneste adresseTjeneste;

    @Before
    public void setUp() {
        UtenlandskMyndighetRepository utenlandskMyndighetRepo = mock(UtenlandskMyndighetRepository.class);
        adresseTjeneste = new AdresseTjeneste(utenlandskMyndighetRepo);

        UtenlandskMyndighet danmark = new UtenlandskMyndighet();
        danmark.land = "Denmark";
        danmark.landkode = Landkoder.DK;

        UtenlandskMyndighet sverige = new UtenlandskMyndighet();
        sverige.land = "Sweden";
        sverige.landkode = Landkoder.SE;

        when(utenlandskMyndighetRepo.findByLandkode(Landkoder.DK)).thenReturn(danmark);
        when(utenlandskMyndighetRepo.findByLandkode(Landkoder.SE)).thenReturn(sverige);
        when(utenlandskMyndighetRepo.findAll()).thenReturn(Arrays.asList(sverige, danmark));
    }

    @Test
    public void hentMyndighet_gyldigLandkode() {
        Response response = adresseTjeneste.hentMyndighet(Landkoder.DK);
        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getEntity()).isInstanceOf(UtenlandskMyndighet.class);

        UtenlandskMyndighet myndighet = (UtenlandskMyndighet) response.getEntity();
        assertThat(myndighet.landkode).isEqualTo(Landkoder.DK);
        assertThat(myndighet.land).isEqualTo("Denmark");
    }

    @Test
    public void hentMyndighet_ikkeGyldigLandkode() {
        Response response = adresseTjeneste.hentMyndighet(Landkoder.NO);
        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNull();
    }

    @Test
    public void hentMyndigheter() {
        Response response = adresseTjeneste.hentMyndigheter();
        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getEntity()).isInstanceOf(List.class);
        
        List myndigheter = (List) response.getEntity();
        assertThat(myndigheter.size()).isEqualTo(2);
        assertThat(myndigheter.get(0)).isInstanceOf(UtenlandskMyndighet.class);
    }
}
