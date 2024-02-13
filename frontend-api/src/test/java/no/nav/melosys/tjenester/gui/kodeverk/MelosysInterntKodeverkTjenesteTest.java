package no.nav.melosys.tjenester.gui.kodeverk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MelosysInterntKodeverkTjeneste.class})
class MelosysInterntKodeverkTjenesteTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/kodeverk/melosys-internt";

    @Test
    void hentKoderTilFolketrygden() throws Exception {
        mockMvc.perform(get(BASE_URL + "/folketrygden")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    }
}

