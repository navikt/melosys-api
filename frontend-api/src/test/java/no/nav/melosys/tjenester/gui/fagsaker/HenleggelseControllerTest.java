package no.nav.melosys.tjenester.gui.fagsaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.HenleggelseService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.HenleggelseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory.lagFagsak;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HenleggelseController.class})
class HenleggelseControllerTest {
    private static final String BASE_URL = "/api/fagsaker";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private BehandlingService behandlingService;
    @MockBean
    private HenleggelseService henleggelseService;

    @Test
    void henleggFagsak() throws Exception {
        String begrunnelseKode = Henleggelsesgrunner.OPPHOLD_UTL_AVLYST.getKode();
        String fritekst = "Dette er fritekst";
        var henleggelseDto = new HenleggelseDto(fritekst, begrunnelseKode);
        String saksnummer = "123";

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(henleggelseDto)))
            .andExpect(status().isNoContent());

        verify(henleggelseService).henleggFagsakEllerBehandling(saksnummer, begrunnelseKode, fritekst);
    }

    @Test
    void henleggSakSomBortfalt() throws Exception {
        long behandlingId = 123;
        Fagsak fagsak = lagFagsak();
        Behandling behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(behandlingId)).thenReturn(behandling);

        mockMvc.perform(put(BASE_URL + "/{behandlingId}/henlegg-som-bortfalt", behandlingId)
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isNoContent());

        verify(henleggelseService).henleggSakEllerBehandlingSomBortfalt(behandlingId);
    }
}
