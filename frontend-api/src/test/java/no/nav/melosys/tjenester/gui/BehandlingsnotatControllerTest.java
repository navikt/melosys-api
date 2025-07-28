package no.nav.melosys.tjenester.gui;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Behandlingsnotat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.BehandlingsnotatService;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatPostDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BehandlingsnotatController.class})
public class BehandlingsnotatControllerTest {

    @MockBean
    private BehandlingsnotatService behandlingsnotatService;
    @MockBean
    private SaksbehandlerService saksbehandlerService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String saksbehandler = "Z224234";
    private static final String BASE_URL = "/api/fagsaker";

    @Test
    void hentBehandlingsnotaterForFagsak() throws Exception {

        final String saksnummer = "MEL-222";
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.hentNotatForFagsak(saksnummer)).thenReturn(List.of(behandlingsnotat));

        mockMvc.perform(get(BASE_URL + "/{saksnummer}/notater", saksnummer)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$[0].endretDato", equalTo(behandlingsnotat.getEndretDato().toString())))
            .andExpect(jsonPath("$[0].notatId", equalTo(behandlingsnotat.getId().intValue())))
            .andExpect(jsonPath("$[0].registrertAvNavn", equalTo(behandlingsnotat.getRegistrertAv())))
            .andExpect(jsonPath("$[0].tekst", equalTo(behandlingsnotat.getTekst())));
    }

    @Test
    void oppdaterBehandlingsnotat() throws Exception {

        final String saksnummer = "MEL-222";
        BehandlingsnotatPostDto dto = new BehandlingsnotatPostDto("teteteksssst");
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.oppdaterNotat(eq(behandlingsnotat.getId()), anyString())).thenReturn(behandlingsnotat);

        mockMvc.perform(put(BASE_URL + "/{saksnummer}/notater/{notatID}", saksnummer, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());

    }

    @Test
    void opprettBehandlingsnotat() throws Exception {

        final String saksnummer = "MEL-222";
        BehandlingsnotatPostDto dto = new BehandlingsnotatPostDto("teteteksssst");
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.opprettNotat(eq(saksnummer), anyString())).thenReturn(behandlingsnotat);

        mockMvc.perform(post(BASE_URL + "/{saksnummer}/notater", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    private Behandlingsnotat lagBehandlingsnotat() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setTekst("hei");
        behandlingsnotat.setRegistrertAv(saksbehandler);
        behandlingsnotat.setEndretAv(saksbehandler);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setId(1L);
        behandlingsnotat.setEndretDato(Instant.now());
        behandlingsnotat.setRegistrertDato(Instant.now());
        behandlingsnotat.setBehandling(behandling);

        return behandlingsnotat;
    }
}
