package no.nav.melosys.tjenester.gui.sak;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.HenleggFagsakService;
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

@WebMvcTest(controllers = {HenleggelseTjeneste.class})
class HenleggelseTjenesteTest {
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
    private HenleggFagsakService henleggFagsakService;

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

        verify(henleggFagsakService).henleggFagsakEllerBehandling(saksnummer, begrunnelseKode, fritekst);
    }

    @Test
    void henleggSakSomBortfalt() throws Exception {
        String saksnummer = "123";
        Fagsak fagsak = lagFagsak(saksnummer);
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        mockMvc.perform(put(BASE_URL + "/{saksnr}/henlegg-som-bortfalt", saksnummer)
                            .contentType(MediaType.TEXT_PLAIN)
                            .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isNoContent());

        verify(henleggFagsakService).henleggSakEllerBehandlingSomBortfalt(saksnummer);
    }
}
