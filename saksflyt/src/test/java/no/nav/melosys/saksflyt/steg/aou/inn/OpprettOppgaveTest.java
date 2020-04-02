package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private OppgaveService oppgaveService;

    private OpprettOppgave opprettOppgave;

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(oppgaveService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        final String journalpostID = "3124";
        final String aktørID = "432523";

        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);

        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}