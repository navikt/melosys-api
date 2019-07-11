package no.nav.melosys.service.abac;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.sikkerhet.abac.Pep;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TilgangServiceTest {
    private TilgangService tilgangService;

    @Mock
    private Pep pep;

    private XacmlResponse abacResponse;

    @Mock
    private BehandlingService behandlingService;

    private Fagsak fagsakMocked;
    private Behandling behandlingMocked;

    @Before
    public void setUp() throws TekniskException {
        AbacContext abacContext = mock(AbacContext.class);
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());

        abacResponse = mock(XacmlResponse.class);

        AbacService abacService = mock(AbacService.class);
        when(abacService.evaluate(any())).thenReturn(abacResponse);

        pep = new PepImpl(abacService, abacContext);

        fagsakMocked = mock(Fagsak.class);
        behandlingMocked = mock(Behandling.class);
        when(fagsakMocked.hentAktørMedRolleType(any())).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);

        tilgangService = new TilgangService(behandlingService, pep);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testBehandlingsIdIkketilgang() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekk(102323934);
    }

    @Test
    public void testBehandlingsIdOk() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekk(102323934);
    }

    @Test
    public void sjekkRedigerbar_behandlingErRedigerbar_Ok() throws FunksjonellException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        when(behandlingMocked.erRedigerbar()).thenReturn(true);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkRedigerbar(123123123);
    }

    @Test(expected = FunksjonellException.class)
    public void sjekkRedigerbar_behandlingIkkeRedigerbar_girFeil() throws FunksjonellException, TekniskException {
        when(behandlingMocked.erRedigerbar()).thenReturn(false);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkRedigerbar(123123123);
    }

    @Test
    public void testFagsakOk() throws SikkerhetsbegrensningException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        tilgangService.sjekkSak(fagsakMocked);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testFagsakIkkeTilgang() throws SikkerhetsbegrensningException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        tilgangService.sjekkSak(fagsakMocked);
    }
}
