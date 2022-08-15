package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.ValgtRepresentantDto;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RepresentantTjeneste.class})
class RepresentantTjenesteTest {
    private static final Logger log = LoggerFactory.getLogger(RepresentantTjenesteTest.class);

    @MockBean
    private RepresentantService representantService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/representant";

    @Test
    void lagreValgtRepresentant_validerResponse() throws Exception {
        var request = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");
        when(representantService.oppdaterValgtRepresentant(eq(1L), any(ValgtRepresentant.class))).thenReturn(request.til());

        mockMvc.perform(post(BASE_URL + "/valgt/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(request, ValgtRepresentantDto.class));
    }

    @Test
    void hentValgtRepresentant_validerResponse() throws Exception {
        var forventetResponse = new ValgtRepresentantDto("repnr", true, "123456789", "kontaktperson");

        when(representantService.hentValgtRepresentant(1L)).thenReturn(forventetResponse.til());

        mockMvc.perform(get(BASE_URL + "/valgt/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(forventetResponse, ValgtRepresentantDto.class));

    }
}
