package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DokumentTjeneste.class})
class DokumentTjenesteTest {

    @MockBean
    private DokumentServiceFasade dokumentServiceFasade;
    @MockBean
    private DokumentHentingService dokumentHentingService;
    @MockBean
    private EessiService eessiService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/dokumenter";


    @Test
    void hentDokument() throws Exception {
        var dokument = new byte[1];
        when(dokumentHentingService.hentDokument(anyString(), anyString())).thenReturn(dokument);

        mockMvc.perform(get(BASE_URL + "/pdf/{journalpostID}/{dokumentID}", "1", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void hentDokumenter() throws Exception {
        when(dokumentHentingService.hentJournalposter(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL + "/oversikt/{saksnummer}", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void produserUtkastBrev() throws Exception {
        var brevBestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();
        when(dokumentServiceFasade.produserUtkast(anyLong(), any())).thenReturn(new byte[1]);

        mockMvc.perform(post(BASE_URL + "/pdf/brev/utkast/{behandlingID}/{produserbartDokument}", 1L, Produserbaredokumenter.MELDING_HENLAGT_SAK)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brevBestillingDto)))
            .andExpect(status().isOk());

        verify(dokumentServiceFasade).produserUtkast(anyLong(), any());
    }

    @Test
    void produserUtkastSed() throws Exception {
        var sedPdfData = new SedPdfData();
        when(eessiService.genererSedPdf(anyLong(), any(SedType.class), any(SedPdfData.class))).thenReturn(new byte[1]);

        mockMvc.perform(post(BASE_URL + "/pdf/sed/utkast/{behandlingID}/{sedType}", 1L, SedType.A003)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sedPdfData)))
            .andExpect(status().isOk());

        verify(eessiService).genererSedPdf(anyLong(), any(SedType.class), any(SedPdfData.class));
    }

    @Test
    void produserDokument() throws Exception {
        var brevBestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        mockMvc.perform(post(BASE_URL + "/opprett/{behandlingID}/{produserbartDokument}", 1L, Produserbaredokumenter.MELDING_HENLAGT_SAK)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brevBestillingDto)))
            .andExpect(status().isNoContent());

        verify(dokumentServiceFasade).produserUtkast(anyLong(), any());
        verify(dokumentServiceFasade).produserDokument(anyLong(), any());
    }

    @Test
    void produserDokumentFeilerMedManglendeMottaker() throws Exception {
        var brevBestillingDto = new BrevbestillingDto.Builder()
            .build();

        mockMvc.perform(post(BASE_URL + "/opprett/{behandlingID}/{produserbartDokument}", 1L, Produserbaredokumenter.MELDING_HENLAGT_SAK)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brevBestillingDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Mottaker trengs for å bestille")));
    }

}
