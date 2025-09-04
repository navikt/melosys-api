package no.nav.melosys.sikkerhet.abac;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PepTest {

    @InjectMocks
    public PepImpl pep;

    @Mock
    private AbacService abacService;

    @Mock
    private AbacContext abacContext;

    @Mock
    private XacmlResponse abacResponse;


    @BeforeEach
    public void setUp() {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
    }

    @Test
    void testSjekkTilgangTilFnr() {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        pep.sjekkTilgangTilFnr("12345678910");
    }

    @Test
    void testSjekkTilgangTilFnrResponsDeny() {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }

    @Test
    void testSjekkTilgangTilFnrResponsIndeterminate() {
        when(abacResponse.getDecision()).thenReturn(Decision.INDETERMINATE);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }

    @Test
    void testSjekkTilgangTilFnrResponsNotApplicable() {
        when(abacResponse.getDecision()).thenReturn(Decision.NOT_APPLICABLE);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }


    @Test
    void testSjekkTilgangTilAktor() {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        pep.sjekkTilgangTilAktoerId("12345678910");
    }

    @Test
    void testSjekkTilgangTilAktorIdResponseDeny() {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }

    @Test
    void testSjekkTilgangTilAktorIdResponseIndeterminate() {
        when(abacResponse.getDecision()).thenReturn(Decision.INDETERMINATE);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }

    @Test
    void testSjekkTilgangTilAktorIdResponseNotApplicable() {
        when(abacResponse.getDecision()).thenReturn(Decision.NOT_APPLICABLE);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> pep.sjekkTilgangTilFnr("12345678910"))
            .withMessageContaining("ikke tilgang");
    }
}
