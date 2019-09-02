package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave opprettOppgave;

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(gsakFasade);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "111");
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "222");

        opprettOppgave.utfør(prosessinstans);

        verify(gsakFasade).opprettOppgave(any(Oppgave.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}