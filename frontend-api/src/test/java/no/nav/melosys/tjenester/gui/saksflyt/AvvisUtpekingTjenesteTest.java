package no.nav.melosys.tjenester.gui.saksflyt;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AvvisUtpekingTjeneste.class})
class AvvisUtpekingTjenesteTest {

    @MockBean
    private UtpekingService utpekingService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/saksflyt/utpeking";

    @Test
    public void avvisUtpeking() throws Exception {

        var dto = new UtpekingAvvisDto();
        dto.setFritekst("test");
        dto.setNyttLovvalgsland("DK");
        dto.setBegrunnelseUtenlandskMyndighet("test");
        dto.setVilSendeAnmodningOmMerInformasjon(false);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/avvis", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(utpekingService).avvisUtpeking(anyLong(), any(UtpekingAvvis.class));
    }
}
