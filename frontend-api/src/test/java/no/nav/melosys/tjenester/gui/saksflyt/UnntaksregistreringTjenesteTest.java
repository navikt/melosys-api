package no.nav.melosys.tjenester.gui.saksflyt;

import no.finn.unleash.Unleash;
import no.nav.melosys.service.UnntaksregistreringService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UnntaksregistreringTjeneste.class})
class UnntaksregistreringTjenesteTest {

    @MockBean
    private UnntaksregistreringService unntaksregistreringService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;


    private static final String BASE_URL = "/api/saksflyt/unntaksregistrering";

    @Test
    void registrerUnntakFraMedlemskap_togglePå_returnererNoContent() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(1L);
        verify(unntaksregistreringService).registrerUnntakFraMedlemskap(1L);
    }
}
