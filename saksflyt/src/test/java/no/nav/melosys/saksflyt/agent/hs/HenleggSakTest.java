package no.nav.melosys.saksflyt.agent.hs;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HenleggSakTest {
    @Mock
    FagsakRepository fagsakRepository;

    @Mock
    BehandlingRepository behandlingRepository;

    @InjectMocks
    HenleggSak henleggSak;

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        Fagsak fagsak = new Fagsak();
        behandling.setFagsak(fagsak);

        henleggSak.utfør(prosessinstans);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        assertThat(fagsak.getStatus()).isEqualTo(Fagsaksstatus.HENLAGT);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.HS_SEND_BREV);
        verify(fagsakRepository).save(fagsak);
        verify(behandlingRepository).save(behandling);
    }
}