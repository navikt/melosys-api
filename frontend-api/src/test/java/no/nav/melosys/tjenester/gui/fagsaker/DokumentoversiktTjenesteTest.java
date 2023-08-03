package no.nav.melosys.tjenester.gui.fagsaker;

import java.util.Collections;

import no.nav.melosys.service.dokument.DokumentHentingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DokumentoversiktTjeneste.class})
class DokumentoversiktTjenesteTest {
    @MockBean
    private DokumentHentingService dokumentHentingService;

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api";

    @Test
    void hentDokumenter() throws Exception {
        when(dokumentHentingService.hentJournalposter(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL + "/fagsaker/{saksnummer}/dokumenter", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
