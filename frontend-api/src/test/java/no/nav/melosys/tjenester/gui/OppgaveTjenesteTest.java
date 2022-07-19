package no.nav.melosys.tjenester.gui;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppgaveTjenesteTest {
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private OppgaveTjeneste oppgaveTjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @BeforeEach
    public void setUp() {
        oppgaveTjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void plukkOppgave() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(1L);
        when(oppgaveService.hentSistAktiveBehandling(anyString())).thenReturn(behandling);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setSaksnummer("MEl-1");
        oppgaveBuilder.setJournalpostId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        Optional<Oppgave> plukket = Optional.of(oppgaveBuilder.build());
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();
        innData.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        when(oppgaveplukker.plukkOppgave(anyString(), eq(innData))).thenReturn(plukket);


        ResponseEntity<?> response = oppgaveTjeneste.plukkOppgave(innData);

        assertThat(response.getBody()).isExactlyInstanceOf(PlukketOppgaveDto.class);
        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getBody();
        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_fnrSendesInn_kallerRettFunksjon() {
        oppgaveTjeneste.søkOppgaverMedPersonIdentEllerOrgnr("fnr", "");

        verify(oppgaveService).finnOppgaverMedPersonIdent("fnr");
        verify(oppgaveService, never()).finnOppgaverMedOrgnr(anyString());
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_orgnrSendesInn_kallerRettFunksjon() {
        oppgaveTjeneste.søkOppgaverMedPersonIdentEllerOrgnr("", "orgnr");

        verify(oppgaveService).finnOppgaverMedOrgnr("orgnr");
        verify(oppgaveService, never()).finnOppgaverMedPersonIdent(anyString());
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_fnrOgOrgnrSendesInn_kasterFeil() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> oppgaveTjeneste.søkOppgaverMedPersonIdentEllerOrgnr("fnr", "orgnr"))
            .withMessageContaining("Fant både personIdent og orgnr. API støtter kun én.");
    }

    @Test
    void søkOppgaverMedPersonIdentEllerOrgnr_ingentingSendesInn_kasterFeil() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> oppgaveTjeneste.søkOppgaverMedPersonIdentEllerOrgnr("", ""))
            .withMessageContaining("Finner ingen søkekriteria");
    }
}
