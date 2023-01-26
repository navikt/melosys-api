package no.nav.melosys.tjenester.gui;

import java.util.Arrays;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdresseTjeneste.class})
class AdresseTjenesteTest {

    @MockBean
    private UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/adresser/myndigheter";

    @BeforeEach
    void setup() {
        UtenlandskMyndighet utenlandskMyndighetDanmark = new UtenlandskMyndighet();
        utenlandskMyndighetDanmark.land = "Denmark";
        utenlandskMyndighetDanmark.landkode = Land_iso2.DK;

        UtenlandskMyndighet utenlandskMyndighetSverige = new UtenlandskMyndighet();
        utenlandskMyndighetSverige.land = "Sweden";
        utenlandskMyndighetSverige.landkode = Land_iso2.SE;

        when(utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.DK, null)).thenReturn(utenlandskMyndighetDanmark);
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.SE, null)).thenReturn(utenlandskMyndighetSverige);
        when(utenlandskMyndighetService.hentAlleUtenlandskMyndigheter()).thenReturn(Arrays.asList(utenlandskMyndighetSverige, utenlandskMyndighetDanmark));
    }

    @Test
    void hentUtenlandskMyndighet_gyldigLandkode() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{landkode}", Landkoder.DK)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.land", equalTo("Denmark")));
    }

    @Test
    void hentUtenlandskMyndighet_ikkeGyldigLandkode() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{landkode}", Landkoder.NO)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void hentMyndigheter() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].land", equalTo("Sweden")))
            .andExpect(jsonPath("$[1].land", equalTo("Denmark")));
    }
}
