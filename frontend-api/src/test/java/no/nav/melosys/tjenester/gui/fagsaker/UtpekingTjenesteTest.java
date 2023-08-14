package no.nav.melosys.tjenester.gui.fagsaker;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.UtpekDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory.lagFagsak;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UtpekingTjeneste.class})
class UtpekingTjenesteTest {
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
    private UtpekingService utpekingService;

    @Test
    void utpekLovvalgsland() throws Exception {
        Fagsak fagsak = lagFagsak();
        UtpekDto utpekDto = new UtpekDto(Set.of("SE:123"), "Fri SED", "Fri brev");
        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/utpek", "123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(utpekDto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang(fagsak);
        verify(utpekingService).utpekLovvalgsland(fagsak, utpekDto.mottakerinstitusjoner(), utpekDto.fritekstSed(), utpekDto.fritekstBrev());
    }
}
