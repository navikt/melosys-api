package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettGsakSakTest {

    @Mock
    private OppgaveService oppgaveService;

    private OpprettGsakSak opprettGsakSak;

    @Before
    public void setup() {
        opprettGsakSak = new OpprettGsakSak(oppgaveService);
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");

        when(oppgaveService.opprettSakForFagsak(any(Fagsak.class), any(Behandlingstyper.class), anyString())).thenReturn(1234L);
        opprettGsakSak.utfør(prosessinstans);

        verify(oppgaveService).opprettSakForFagsak(any(Fagsak.class), any(Behandlingstyper.class), anyString());
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID)).isEqualTo("1234");
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST);
    }
}