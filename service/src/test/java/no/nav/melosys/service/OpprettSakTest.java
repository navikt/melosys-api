package no.nav.melosys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Bruker;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;

public class OpprettSakTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private FagsakRepository fagsakRepo;

    @Mock
    private BehandlingRepository behandlingRepo;

    OpprettSakService service;

    @Before
    public void setUp() {
        gsakFasade = mock(GsakFasade.class);
        tpsFasade = mock(TpsFasade.class);
        fagsakRepo = mock(FagsakRepository.class);
        behandlingRepo = mock(BehandlingRepository.class);

        service = new OpprettSakService(gsakFasade, tpsFasade, fagsakRepo, behandlingRepo);
    }

    @Test
    public void opprettTest() {
        String fnr = "FJERNET";
        when(gsakFasade.opprettSak(any(), eq(fnr))).thenReturn("1234");

        when(tpsFasade.hentAktørIdForIdent(any())).thenReturn(Optional.of(123L));
        Bruker b = new Bruker();
        when(tpsFasade.hentKjerneinformasjon(any())).thenReturn(b);

        Fagsak fagsak = service.opprettSak(fnr);

        verify(fagsakRepo, times(2)).save(any(Fagsak.class));
        verify(behandlingRepo, times(1)).save(any(Behandling.class));
        assertThat(fagsak.getSaksnummer()).isEqualTo(Long.parseLong("1234"));
        assertThat(fagsak.getStatus()).isEqualTo(FagsakStatus.UBEH);
    }

}
