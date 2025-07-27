package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EessiController.class})
public class EessiControllerTest {

    @MockBean
    private EessiService eessiService;
    @MockBean
    private BehandlingService behandlingService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/eessi";
    private final String mottakerBelgia = "BE:12222";
    private final Institusjon institusjonBelgia = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());

    @Test
    void hentMottakerinstitusjoner() throws Exception {
        when(eessiService.hentEessiMottakerinstitusjoner(anyString(), anyCollection())).thenReturn(singletonList(institusjonBelgia));

        mockMvc.perform(get(BASE_URL + "/mottakerinstitusjoner/{bucType}", BucType.LA_BUC_01)
                .contentType(MediaType.APPLICATION_JSON)
                .param("landkoder", Landkoder.BE.getKode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].landkode", equalTo(Landkoder.BE.getKode())));
    }

    @Test
    void opprettBuc() throws Exception {
        var dto = new BucBestillingDto(BucType.LA_BUC_01, singletonList(Landkoder.BE.getKode()), emptyList());
        when(eessiService.opprettBucOgSed(anyLong(), any(), anyList(), anyCollection())).thenReturn("url");

        mockMvc.perform(post(BASE_URL + "/bucer/{behandlingID}/opprett", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    @Test
    void hentBucer() throws Exception {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());

        mockMvc.perform(get(BASE_URL + "/bucer/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .param("statuser", "A"))
            .andExpect(status().isOk());
    }

    private static Behandling lagBehandling() {
        var behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setId(1L);
        behandling.setFagsak(FagsakTestFactory.builder().medGsakSaksnummer().build());
        return behandling;
    }
}

