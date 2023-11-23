package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift;

import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TrygdeavgiftTjeneste.class})
class TrygdeavgiftTjenesteTest {
    @MockBean
    private static Aksesskontroll aksesskontroll;
    @MockBean
    private static TrygdeavgiftOppsummeringService trygdeavgiftOppsummeringService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void hentTrygdeavgiftOppsummering() throws Exception {
        mockMvc.perform(get("/api/fagsaker/{saksnr}/trygdeavgift/oppsummering", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(aksesskontroll).autoriserSakstilgang("123");
        verify(trygdeavgiftOppsummeringService).harFagsakBehandlingerMedTrygdeavgift("123");
    }
}
