package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AktoerTjeneste.class})
class AktoerTjenesteTest {

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private AktoerService aktoerService;
    @MockBean
    private FagsakService fagsakService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void lagOppdaterAktoer() throws Exception {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1235");
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setOrgnr("123456789");
        aktoerDto.setDatabaseID(1L);

        Fagsak fagsak = lagFagsak();


        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(fagsak);
        when(aktoerService.lagEllerOppdaterAktoer(any(Fagsak.class), any(AktoerDto.class))).thenReturn(1L);


        mockMvc.perform(post("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aktoerDto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(aktoerDto, AktoerDto.class));
    }

    @Test
    void hentAktoer_representantOgFullmektig_representant_tilAktoerDto() throws Exception {
        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        Aktoer fullmektig = new Aktoer();
        fullmektig.setRolle(Aktoersroller.FULLMEKTIG);
        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());
        when(aktoerService.hentfagsakAktører(any(), eq(Aktoersroller.REPRESENTANT), any())).thenReturn(new ArrayList<>(List.of(representant)));
        when(aktoerService.hentfagsakAktører(any(), eq(Aktoersroller.FULLMEKTIG), any())).thenReturn(new ArrayList<>(List.of(fullmektig)));


        mockMvc.perform(get("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .param("rolleKode", Aktoersroller.REPRESENTANT.getKode())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(List.of(AktoerDto.tilDto(representant), AktoerDto.tilDto(fullmektig)), new TypeReference<List<AktoerDto>>() {
            }));
    }

    @Test
    void hentAktoer_representantOgFullmektig_fullmektig_tilAktoerDto() throws Exception {
        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        Aktoer fullmektig = new Aktoer();
        fullmektig.setRolle(Aktoersroller.FULLMEKTIG);
        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());
        when(aktoerService.hentfagsakAktører(any(), eq(Aktoersroller.REPRESENTANT), any())).thenReturn(new ArrayList<>(List.of(representant)));
        when(aktoerService.hentfagsakAktører(any(), eq(Aktoersroller.FULLMEKTIG), any())).thenReturn(new ArrayList<>(List.of(fullmektig)));


        mockMvc.perform(get("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .param("rolleKode", Aktoersroller.FULLMEKTIG.getKode())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(List.of(AktoerDto.tilDto(fullmektig), AktoerDto.tilDto(representant)), new TypeReference<List<AktoerDto>>() {
            }));
    }

    @Test
    void hentAktoer_tilAktoerDto() throws Exception {
        Aktoer aktoerMyndighet = new Aktoer();
        aktoerMyndighet.setId(39L);
        aktoerMyndighet.setAktørId("1235");
        aktoerMyndighet.setInstitusjonId("INST2");
        aktoerMyndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktoerMyndighet.setOrgnr("100");
        aktoerMyndighet.setFagsak(lagFagsak());
        aktoerMyndighet.setUtenlandskPersonId("UTL");


        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());
        when(aktoerService.hentfagsakAktører(any(), any(), any())).thenReturn(Collections.singletonList(aktoerMyndighet));


        mockMvc.perform(get("/api/fagsaker/{saksnummer}/aktoerer", "MELTEST-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(Collections.singletonList(AktoerDto.tilDto(aktoerMyndighet)), new TypeReference<List<AktoerDto>>() {
            }));
    }

    @Test
    void slettAktoer_ok() throws Exception {
        mockMvc.perform(delete("/api/fagsaker/aktoerer/{databaseID}", 123L))
            .andExpect(status().isNoContent());
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}
