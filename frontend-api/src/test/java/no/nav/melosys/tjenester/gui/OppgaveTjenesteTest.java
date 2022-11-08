package no.nav.melosys.tjenester.gui;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OppgaveTjeneste.class})
class OppgaveTjenesteTest {

    @MockBean
    private Oppgaveplukker oppgaveplukker;
    @MockBean
    private OppgaveService oppgaveService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/oppgaver";

    @BeforeEach
    public void setUp() {
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void plukkOppgave() throws Exception {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setSaksnummer("MEl-1");
        oppgaveBuilder.setJournalpostId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        Optional<Oppgave> plukket = Optional.of(oppgaveBuilder.build());

        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto(null, null, Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(1L);

        when(oppgaveplukker.plukkOppgave(any(), any())).thenReturn(plukket);
        when(oppgaveService.hentSistAktiveBehandling(anyString())).thenReturn(behandling);

        PlukketOppgaveDto response = new PlukketOppgaveDto();
        response.setBehandlingID(behandling.getId());
        response.setBehandlingstype(behandling.getType().getKode());
        response.setBehandlingstema(behandling.getTema().getKode());
        response.setJournalpostID(plukket.get().getJournalpostId());
        response.setOppgaveID(plukket.get().getOppgaveId());
        response.setSaksnummer(plukket.get().getSaksnummer());

        mockMvc.perform(post(BASE_URL + "/plukk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(response, PlukketOppgaveDto.class));
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_fnrSendesInn_kallerRettFunksjon() throws Exception {
        mockMvc.perform(get(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .param("personIdent", "fnr")
            )
            .andExpect(status().isOk());


        verify(oppgaveService).finnBehandlingsoppgaverMedPersonIdent("fnr");
        verify(oppgaveService, never()).finnBehandlingsoppgaverMedOrgnr(anyString());
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_orgnrSendesInn_kallerRettFunksjon() throws Exception {
        mockMvc.perform(get(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .param("orgnr", "orgnr")
            )
            .andExpect(status().isOk());

        verify(oppgaveService).finnBehandlingsoppgaverMedOrgnr("orgnr");
        verify(oppgaveService, never()).finnBehandlingsoppgaverMedPersonIdent(anyString());
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_fnrOgOrgnrSendesInn_kasterFeil() throws Exception {
        mockMvc.perform(get(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .param("personIdent", "fnr")
                .param("orgnr", "orgnr")
            )
            .andExpect(status().is4xxClientError())
            .andExpect(responseBody(objectMapper)
                .containsError("message", "Fant både personIdent og orgnr. API støtter kun én."));
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_ingentingSendesInn_kasterFeil() throws Exception {
        mockMvc.perform(get(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is4xxClientError())
            .andExpect(responseBody(objectMapper)
                .containsError("message", "Finner ingen søkekriteria. API støtter personIdent(fnr eller dnr) og orgnr"));
    }
}
