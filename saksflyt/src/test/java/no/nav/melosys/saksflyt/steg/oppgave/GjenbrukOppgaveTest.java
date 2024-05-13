package no.nav.melosys.saksflyt.steg.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GjenbrukOppgaveTest {

    @Mock
    private OppgaveService oppgaveService;
    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private GjenbrukOppgave gjenbrukOppgave;

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory();

    @BeforeEach
    public void setUp() {
        gjenbrukOppgave = new GjenbrukOppgave(oppgaveService);
    }

    @Test
    void gjenbrukOppgave_utfør_oppdatererOppgave() {
        final String oppgaveID = "1234";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave);
        var prosessinstans = lagProsessinstans(oppgaveID, false);
        when(oppgaveService.lagBehandlingsoppgave(any())).thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.getBehandling(), LocalDate.now(), () -> null));


        gjenbrukOppgave.utfør(prosessinstans);


        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", FagsakTestFactory.SAKSNUMMER)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.getKode())
            .hasFieldOrPropertyWithValue("behandlingstype", null)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("aktørId", "123321");
    }

    @Test
    void gjenbrukOppgave_utfør_oppdatererOppgave_virksomhet() {
        final String oppgaveID = "1234";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave);
        var prosessinstans = lagProsessinstans(oppgaveID, true);
        when(oppgaveService.lagBehandlingsoppgave(any())).thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.getBehandling(), LocalDate.now(), () -> null));


        gjenbrukOppgave.utfør(prosessinstans);


        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", FagsakTestFactory.SAKSNUMMER)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.getKode())
            .hasFieldOrPropertyWithValue("behandlingstype", null)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("orgnr", "999999999");
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, Boolean erForVirksomhet) {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Prosessinstans prosessinstans = new Prosessinstans();

        if (erForVirksomhet) {
            Aktoer virksomhet = new Aktoer();
            virksomhet.setOrgnr("999999999");
            virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
            fagsak.leggTilAktør(virksomhet);
        } else {
            Aktoer bruker = new Aktoer();
            bruker.setAktørId("123321");
            bruker.setRolle(Aktoersroller.BRUKER);
            fagsak.leggTilAktør(bruker);
        }

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "Deg321");
        return prosessinstans;
    }
}
