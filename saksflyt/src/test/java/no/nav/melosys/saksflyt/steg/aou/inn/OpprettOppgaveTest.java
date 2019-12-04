package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
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

    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave opprettOppgave;

    @Captor
    private ArgumentCaptor<Oppgave> captor;

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(gsakFasade);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "111");
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "222");

        opprettOppgave.utfør(prosessinstans);

        verify(gsakFasade).opprettOppgave(captor.capture());
        Oppgave oppgave = captor.getValue();
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.BEH_SED);
        assertThat(oppgave.getBehandlingstype()).isEqualTo(behandling.getType());
    }
}