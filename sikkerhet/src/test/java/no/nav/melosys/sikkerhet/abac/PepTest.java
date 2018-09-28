package no.nav.melosys.sikkerhet.abac;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PepTest {

    @InjectMocks
    public PepImpl pep;

    @Mock
    private AbacService abacService;

    @Mock
    private AbacContext abacContext;

    @Mock
    private XacmlResponse abacResponse;


    @Before
    public void setUp() {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
    }

    public void testSjekkTilgangTilFnr() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        pep.sjekkTilgangTilFnr("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilFnrResponsDeny() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        pep.sjekkTilgangTilFnr("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilFnrResponsIndeterminate() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.INDETERMINATE);
        pep.sjekkTilgangTilFnr("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilFnrResponsNotApplicable() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.NOT_APPLICABLE);
        pep.sjekkTilgangTilFnr("12345678910");
    }


    @Test()
    public void testSjekkTilgangTilAktor() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        pep.sjekkTilgangTilAktoerId("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilAktorIdResponseDeny() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        pep.sjekkTilgangTilAktoerId("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilAktorIdResponseIndeterminate() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.INDETERMINATE);
        pep.sjekkTilgangTilAktoerId("12345678910");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testSjekkTilgangTilAktorIdResponseNotApplicable() throws SikkerhetsbegrensningException {
        when(abacResponse.getDecision()).thenReturn(Decision.NOT_APPLICABLE);
        pep.sjekkTilgangTilAktoerId("12345678910");
    }
}
