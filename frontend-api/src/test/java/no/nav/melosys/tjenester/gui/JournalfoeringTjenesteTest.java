package no.nav.melosys.tjenester.gui;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JournalfoeringTjenesteTest {
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EasyRandom random;

    @Mock
    private JournalfoeringService journalføringService;
    @Mock
    private OppgaveService oppgaveService;

    private JournalfoeringTjeneste tjeneste;

    @BeforeEach
    public void setUp() {
        tjeneste = new JournalfoeringTjeneste(journalføringService, oppgaveService);

        random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));
    }

    @Test
    void journalførOgKnyttTilSak_validerKall() {
        JournalfoeringTilordneDto journalføringDto = random.nextObject(JournalfoeringTilordneDto.class);

        tjeneste.journalførOgKnyttTilSak(journalføringDto);

        verify(journalføringService).journalførOgKnyttTilEksisterendeSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalførOgOpprettNyVurdering_validerKall() {
        JournalfoeringTilordneDto journalføringDto = random.nextObject(JournalfoeringTilordneDto.class);

        tjeneste.journalførOgOpprettNyVurdering(journalføringDto);

        verify(journalføringService).journalførOgOpprettNyVurdering(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKallOgSchema() {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(null);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalføringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalføringDto.setRepresentantID(SAMPLE_ORGNR);

        tjeneste.journalførOgOpprettSak(journalføringDto);

        verify(journalføringService).journalførOgOpprettSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKallOgSchemaMedRepresentantIDNull() {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(null);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalføringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalføringDto.setRepresentantID(null);

        tjeneste.journalførOgOpprettSak(journalføringDto);

        verify(journalføringService).journalførOgOpprettSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalføringOpprett_validerKallOgSchemaMedBrukerIDNull() {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setVirksomhetOrgnr(SAMPLE_ORGNR);
        journalføringDto.setBrukerID(null);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalføringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalføringDto.setRepresentantID(SAMPLE_ORGNR);

        tjeneste.journalførOgOpprettSak(journalføringDto);

        verify(journalføringService).journalførOgOpprettSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
    }

    @Test
    void journalførSed_validerSchema() {
        JournalfoeringSedDto journalføringSedDto = new JournalfoeringSedDto();
        journalføringSedDto.setOppgaveID("123123");
        journalføringSedDto.setBrukerID(SAMPLE_FNR);
        journalføringSedDto.setJournalpostID("1231231232");

        tjeneste.journalførSed(journalføringSedDto);

        verify(journalføringService).journalførSed(journalføringSedDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringSedDto.getOppgaveID());
    }
}

