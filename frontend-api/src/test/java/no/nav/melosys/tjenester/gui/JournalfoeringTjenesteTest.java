package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {JournalfoeringTjeneste.class})
class JournalfoeringTjenesteTest {
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EasyRandom random;

    @MockBean
    private JournalfoeringService journalføringService;
    @MockBean
    private OppgaveService oppgaveService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/journalforing";

    @BeforeEach
    public void setUp() {
        random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));
    }

    @Test
    void journalførOgKnyttTilSak_validerKall() throws Exception {
        JournalfoeringTilordneDto journalføringDto = random.nextObject(JournalfoeringTilordneDto.class);

        mockMvc.perform(post(BASE_URL + "/knytt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførOgKnyttTilEksisterendeSak(any(JournalfoeringTilordneDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalførOgOpprettAndregangsBehandling_validerKall() throws Exception {
        JournalfoeringTilordneDto journalføringDto = random.nextObject(JournalfoeringTilordneDto.class);

        mockMvc.perform(post(BASE_URL + "/nyvurdering")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførOgOpprettAndregangsBehandling(any(JournalfoeringTilordneDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKall() throws Exception {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(null);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());

        mockMvc.perform(post(BASE_URL + "/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførOgOpprettSak(any(JournalfoeringOpprettDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKallMedFullmektigIDNull() throws Exception {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(null);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());

        mockMvc.perform(post(BASE_URL + "/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførOgOpprettSak(any(JournalfoeringOpprettDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKallMedBrukerIDNull() throws Exception {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(SAMPLE_ORGNR);
        journalføringDto.setBrukerID(null);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());

        mockMvc.perform(post(BASE_URL + "/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførOgOpprettSak(any(JournalfoeringOpprettDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalførSed_validerKall() throws Exception {
        JournalfoeringSedDto journalføringSedDto = new JournalfoeringSedDto();
        journalføringSedDto.setOppgaveID("123123");
        journalføringSedDto.setBrukerID(SAMPLE_FNR);
        journalføringSedDto.setJournalpostID("1231231232");

        mockMvc.perform(post(BASE_URL + "/sed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringSedDto))
            )
            .andExpect(status().isNoContent());

        verify(journalføringService).journalførSed(any(JournalfoeringSedDto.class));
        verify(oppgaveService).ferdigstillOppgave(journalføringSedDto.getOppgaveID());
    }
}

