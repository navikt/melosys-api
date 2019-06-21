package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.*;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingOpprettetTest {

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;

    private OppdaterSakOgBehandlingOpprettet oppdaterSakOgBehandlingOpprettet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingOpprettet = new OppdaterSakOgBehandlingOpprettet(sakOgBehandlingFasade);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");

        oppdaterSakOgBehandlingOpprettet.utfør(prosessinstans);
        verify(sakOgBehandlingFasade).sendBehandlingOpprettet(any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_FERDIGSTILL_JOURNALPOST);
    }
}