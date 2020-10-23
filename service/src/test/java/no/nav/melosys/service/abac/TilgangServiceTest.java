package no.nav.melosys.service.abac;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
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

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class TilgangServiceTest {
    private TilgangService tilgangService;

    @Mock
    private Pep pep;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private XacmlResponse abacResponse;
    private AbacContext abacContext;
    private AbacService abacService;

    private Fagsak fagsakMocked;
    private Behandling behandlingMocked;
    private final String saksnummer = "MEL-111";

    @BeforeEach
    public void setUp() throws TekniskException, IkkeFunnetException {
        abacContext = mock(AbacContext.class);

        abacResponse = mock(XacmlResponse.class);

        abacService = mock(AbacService.class);

        pep = new PepImpl(abacService, abacContext);

        fagsakMocked = mock(Fagsak.class);
        behandlingMocked = mock(Behandling.class);

        tilgangService = new TilgangService(fagsakService, behandlingService, oppgaveService, pep);
    }

    @Test
    public void testBehandlingsIdIkketilgang() throws Exception {
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
    public void testBehandlingsIdOk() throws Exception {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkTilgang(102323934);
    }

    @Test
    public void sjekkRedigerbar_behandlingErRedigerbar_Ok() throws FunksjonellException, TekniskException {
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
    public void sjekkRedigerbar_behandlingIkkeRedigerbar_girFeil() throws FunksjonellException, TekniskException {
        when(behandlingMocked.erRedigerbar()).thenReturn(false);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilgangService.sjekkRedigerbarOgTilgang(123123123))
            .withMessage("Forsøk på å endre en ikke-redigerbar behandling med id 123123123");
    }

    @Test
    public void sjekkTilordnet_behandlingErTilordnetSaksbehandler_Ok() throws FunksjonellException, TekniskException {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        Oppgave oppgave = new Oppgave.Builder().setTilordnetRessurs(saksbehandler).build();

        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        when(behandlingMocked.erRedigerbar()).thenReturn(true);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(any())).thenReturn(Optional.of(oppgave));

        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123);
    }

    @Test
    public void sjekkTilordnet_behandlingErIkkeTilordnetSaksbehandler_girFeil() throws FunksjonellException, TekniskException {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        Oppgave oppgave = new Oppgave.Builder().setTilordnetRessurs("Z000000").build();

        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(any())).thenReturn(Optional.of(oppgave));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123))
            .withMessage("Forsøk på å endre behandling med id 123123123 som ikke er tilordnet Z123456");
    }

    @Test
    public void testFagsakOk() throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());
        when(abacService.evaluate(any())).thenReturn(abacResponse);
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMocked);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());

        tilgangService.sjekkSak(saksnummer);
    }

    @Test
    public void testFagsakIkkeTilgang() throws TekniskException, IkkeFunnetException {
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
