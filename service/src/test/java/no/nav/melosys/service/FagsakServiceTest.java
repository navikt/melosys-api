package no.nav.melosys.service;

import java.time.Instant;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class FagsakServiceTest {

    private FagsakService fagsakService;

    @Before
    public void setUp() {
        TpsFasade tps = mock(TpsFasade.class);
        FagsakRepository fagsakRepo = mock(FagsakRepository.class);
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        fagsakService = new FagsakService(fagsakRepo, behandlingRepository, behandlingsresultatRepository, tps);
    }

    @Test
    public void lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(Fagsaksstatus.OPPRETTET);
        fagsak.setType(Fagsakstype.EU_EØS);
        fagsak.setRegistrertDato(Instant.now());

        fagsakService.lagre(fagsak);
        assertNotNull(fagsak);
        assertNotNull(fagsak.getSaksnummer());
    }

    @Test
    public void nyFagsak() {
        final String[] identer = new String[]{"88888888884", "77777777779"};

        for (String fnr : identer) {
            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(fnr, "123456789", "", Behandlingstype.SØKNAD);
            assertNotNull(fagsak);
            assertFalse(fagsak.getBehandlinger().isEmpty());
        }
    }
}