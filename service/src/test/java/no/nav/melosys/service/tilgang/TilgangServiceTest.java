package no.nav.melosys.service.tilgang;

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
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.abac.Pep;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilgangServiceTest {
    private TilgangService tilgangService;

    @Mock
    private Pep pep;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private XacmlResponse abacResponse;
    private AbacContext abacContext;
    private AbacService abacService;

    private Fagsak fagsakMocked;
    private Behandling behandlingMocked;
    private final String saksnummer = "MEL-111";

    @BeforeEach
    void setUp() {
        abacContext = mock(AbacContext.class);

        abacResponse = mock(XacmlResponse.class);

        abacService = mock(AbacService.class);

        pep = new PepImpl(abacService, abacContext);

        fagsakMocked = mock(Fagsak.class);
        behandlingMocked = mock(Behandling.class);

        tilgangService = new TilgangService(fagsakService, behandlingService, pep);
    }

    @Test
    void testBehandlingsIdIkketilgang() throws Exception {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> tilgangService.sjekkTilgang(102323934))
            .withMessage("ABAC: Brukeren har ikke tilgang til ressurs");
    }

    @Test
    void testBehandlingsIdOk() throws Exception {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkTilgang(102323934);
    }

    @Test
    void sjekkRedigerbar_behandlingErRedigerbar_Ok() {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(behandlingMocked.erRedigerbar()).thenReturn(true);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkRedigerbarOgTilgang(123123123);
    }

    @Test
    void sjekkRedigerbar_behandlingIkkeRedigerbar_girFeil() {
        when(behandlingMocked.erRedigerbar()).thenReturn(false);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilgangService.sjekkRedigerbarOgTilgang(123123123))
            .withMessage("Forsøk på å endre en ikke-redigerbar behandling med id 123123123");
    }

    @Test
    void sjekkTilordnet_behandlingErTilordnetSaksbehandler_Ok() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);

        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandlingMocked, saksbehandler)).thenReturn(true);

        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123);
    }

    @Test
    public void sjekkTilordnet_behandlingErIkkeTilordnetSaksbehandler_girFeil() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);

        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123))
            .withMessage("Forsøk på å endre behandling med id 123123123 som er ikke-redigerbar eller ikke er tilordnet Z123456");
    }

    @Test
    void testFagsakOk() {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMocked);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());

        tilgangService.sjekkSak(saksnummer);
    }

    @Test
    void testFagsakIkkeTilgang() {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMocked);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());

        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() ->tilgangService.sjekkSak(saksnummer))
            .withMessage("ABAC: Brukeren har ikke tilgang til ressurs");
    }
}
