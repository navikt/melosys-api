package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    private OpprettOppgave opprettOppgave;

    @Mock
    private GsakFasade gsakFasade;

    @Captor
    private ArgumentCaptor<Oppgave> captor;

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(gsakFasade);
    }

    @Test
    public void utfør() throws MelosysException {
        final String saksnummer = "MELIMELL-99";
        final String aktørID = "32313";

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.UTL_MYND_UTPEKT_NORGE);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer(saksnummer);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        opprettOppgave.utfør(prosessinstans);

        verify(gsakFasade).opprettOppgave(captor.capture());
        Oppgave opprettetOppgave = captor.getValue();
        assertThat(opprettetOppgave.getAktørId()).isEqualTo(aktørID);
        assertThat(opprettetOppgave.getSaksnummer()).isEqualTo(saksnummer);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}