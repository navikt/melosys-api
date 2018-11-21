package no.nav.melosys.service;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.Tilgang;
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
public class TilgangTest {
    private Tilgang tilgang;

    @Mock
    private Pep pep;

    private XacmlResponse abacResponse;

    @Mock
    private BehandlingRepository behandlingRepository;

    private Fagsak fagsakMocked;
    private Behandling behandlingMocked;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
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

        tilgang = new Tilgang(behandlingRepository, pep);
    }

    @Test(expected = IkkeFunnetException.class)
    public void testBehandlingsIdIkkeKnyttetTilFagsak() throws Throwable {
        tilgang.sjekk(102323934);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testBehandlingsIdIkketilgang() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);

        when(behandlingRepository.findOne(anyLong())).thenReturn(behandlingMocked);

        tilgang.sjekk(102323934);
    }

    @Test
    public void testBehandlingsIdOk() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        when(behandlingRepository.findOne(anyLong())).thenReturn(behandlingMocked);

        tilgang.sjekk(102323934);
    }

    @Test
    public void testFagsakOk() throws SikkerhetsbegrensningException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        tilgang.sjekkSak(fagsakMocked);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testFagsakIkkeTilgang() throws SikkerhetsbegrensningException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        tilgang.sjekkSak(fagsakMocked);
    }
}
