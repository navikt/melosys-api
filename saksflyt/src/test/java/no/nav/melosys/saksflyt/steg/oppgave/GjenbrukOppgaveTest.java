package no.nav.melosys.saksflyt.steg.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
        final String saksnummer = "MEL-123";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave);
        var prosessinstans = lagProsessinstans(oppgaveID, saksnummer, false);
        when(oppgaveService.lagBehandlingsoppgave(any())).thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.getBehandling(), LocalDate.now(), () -> null));


        gjenbrukOppgave.utfør(prosessinstans);


        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", saksnummer)
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
        final String saksnummer = "MEL-123";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave);
        var prosessinstans = lagProsessinstans(oppgaveID, saksnummer, true);
        when(oppgaveService.lagBehandlingsoppgave(any())).thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.getBehandling(), LocalDate.now(),  () -> null));


        gjenbrukOppgave.utfør(prosessinstans);


        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", saksnummer)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.getKode())
            .hasFieldOrPropertyWithValue("behandlingstype", null)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("orgnr", "999999999");
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, String saksnummer, Boolean erForVirksomhet) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        Prosessinstans prosessinstans = new Prosessinstans();

        if (erForVirksomhet) {
            Aktoer virksomhet = new Aktoer();
            virksomhet.setOrgnr("999999999");
            virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
            fagsak.getAktører().add(virksomhet);
        } else {
            Aktoer bruker = new Aktoer();
            bruker.setAktørId("123321");
            bruker.setRolle(Aktoersroller.BRUKER);
            fagsak.getAktører().add(bruker);
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
