package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
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
    @Mock
    private TpsService tpsService;

    private OppdaterSakOgBehandlingOpprettet oppdaterSakOgBehandlingOpprettet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingOpprettet = new OppdaterSakOgBehandlingOpprettet(sakOgBehandlingFasade, tpsService, null);
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
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE);
    }
}