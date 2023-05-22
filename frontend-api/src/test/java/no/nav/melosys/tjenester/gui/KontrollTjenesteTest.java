package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto;
import no.nav.melosys.tjenester.gui.kontroll.KontrollTjeneste;
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

@WebMvcTest(controllers = {KontrollTjeneste.class})
public class KontrollTjenesteTest {

    @MockBean
    private FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private EessiService eessiService;
    @MockBean
    private BehandlingService behandlingService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/kontroll";

    @Test
    void kontrollerFerdigbehandling() throws Exception {
        var dto = new FerdigbehandlingKontrollerDto(1L, Vedtakstyper.FØRSTEGANGSVEDTAK,
            Behandlingsresultattyper.HENLEGGELSE, null, false);

        mockMvc.perform(post(BASE_URL + "/ferdigbehandling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    void kontrollerFerdigbehandlingUtenVedtakstypeGirBadRequest() throws Exception {
        var dto = new FerdigbehandlingKontrollerDto(1L, null, Behandlingsresultattyper.HENLEGGELSE,
            null, false);

        mockMvc.perform(post(BASE_URL + "/ferdigbehandling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Vedtakstype mangler.")));
    }
}
