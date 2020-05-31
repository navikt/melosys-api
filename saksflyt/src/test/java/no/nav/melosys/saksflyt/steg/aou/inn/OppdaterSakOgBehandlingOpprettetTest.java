package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.sob.SobService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingOpprettetTest {

    @Mock
    private SobService sobService;

    private OppdaterSakOgBehandlingOpprettet oppdaterSakOgBehandlingOpprettet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingOpprettet = new OppdaterSakOgBehandlingOpprettet(sobService);
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
        verify(sobService).sakOgBehandlingOpprettet(eq("123"), eq(1L), eq("123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE);
    }
}