package no.nav.melosys.tjenester.gui;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {KontaktopplysningController.class})
class KontaktopplysningControllerTest {

    @MockBean
    private KontaktopplysningService kontaktopplysningService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/fagsaker";

    private static final String SAK_NUMMER = "MEL-1";
    private static final String ORG_NUMMER = "999";
    private static final KontaktInfoDto KONTAKT_INFO = new KontaktInfoDto("kontaktnavn", "kontaktorgnr", "kontakttelefon");

    @Test
    void hentKontaktopplysning_kallerPåService_objektFinnes() throws Exception {
        when(kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER)).thenReturn(Optional.of(new Kontaktopplysning()));

        mockMvc.perform(get(BASE_URL + "/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }

    @Test
    void hentKontaktopplysning_kallerPåService_objektFinnesIkke() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());

        verify(kontaktopplysningService).hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);
    }

    @Test
    void lagKontaktopplysning_kallerPåService() throws Exception {
        Kontaktopplysning kontaktopplysning = Kontaktopplysning.av(ORG_NUMMER, KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon(), KONTAKT_INFO.kontaktorgnr());
        when(kontaktopplysningService.lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO.kontaktorgnr(), KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon())).thenReturn(kontaktopplysning);

        mockMvc.perform(post(BASE_URL + "/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(KONTAKT_INFO))
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(kontaktopplysning, Kontaktopplysning.class));
        verify(kontaktopplysningService, times(1))
            .lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO.kontaktorgnr(), KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon());
    }

    @Test
    void slettKontaktopplysning_kallerPåService() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());

        verify(kontaktopplysningService).slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);
    }
}
