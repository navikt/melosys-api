package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.SakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettJournalfoeringsOppgaveTest {

    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private SakService sakService;

    private OpprettJournalfoeringsOppgave opprettJournalfoeringsOppgave;

    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private static final String JOURNALPOST_ID = "12333211";
    private static final String AKTØR_ID = "333333";


    @Before
    public void setup() {
        opprettJournalfoeringsOppgave = new OpprettJournalfoeringsOppgave(oppgaveService, sakService);
    }

    @Test
    public void utfør_behandlingEksisterer_hentTemaFraSak() throws MelosysException {
        final String saksnummer = "123321";
        final Long gsakSaksnummer = 222L;
        Prosessinstans prosessinstans = hentProsessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        Tema tema = Tema.UFM;
        when(sakService.hentTemaFraSak(eq(gsakSaksnummer))).thenReturn(tema);

        opprettJournalfoeringsOppgave.utfør(prosessinstans);
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        Oppgave oppgave = oppgaveCaptor.getValue();
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.JFR);
        assertThat(oppgave.getTema()).isEqualTo(Tema.UFM);

    }

    @Test
    public void utfør_ingenEksisterendeSakNySed_temaMEDFraSedType() throws MelosysException {
        Prosessinstans prosessinstans = hentProsessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(SedType.A005.name());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        opprettJournalfoeringsOppgave.utfør(prosessinstans);
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        Oppgave oppgave = oppgaveCaptor.getValue();
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.JFR);
        assertThat(oppgave.getTema()).isEqualTo(Tema.MED);
    }



    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);

        return prosessinstans;
    }
}