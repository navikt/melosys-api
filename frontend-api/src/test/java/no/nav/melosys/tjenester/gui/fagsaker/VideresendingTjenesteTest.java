package no.nav.melosys.tjenester.gui.fagsaker;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.VideresendSoknadService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.VideresendDto;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {VideresendingTjeneste.class})
class VideresendingTjenesteTest {
    private static final String BASE_URL = "/api/fagsaker";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private FagsakService fagsakService;
    @MockBean
    private VideresendSoknadService videresendSoknadService;

    @Test
    void videresend() throws Exception {
        var videresendDto = new VideresendDto();
        videresendDto.setVedlegg(Collections.singletonList(new VedleggDto("1", "2")));
        videresendDto.setMottakerinstitusjon("Denmark");
        videresendDto.setFritekst("Dette er fritekst");

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg-videresend", "123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(videresendDto)))
            .andExpect(status().isNoContent());
    }

    @Test
    void videresend_utenVedlegg_feiler() throws Exception {
        var videresendDto = new VideresendDto();

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg-videresend", "123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(videresendDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("uten vedlegg")));
    }
}
