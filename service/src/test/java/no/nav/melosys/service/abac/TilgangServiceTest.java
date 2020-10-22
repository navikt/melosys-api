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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
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


    private Fagsak fagsakMocked;
    private Behandling behandlingMocked;
    private final String saksnummer = "MEL-111";

    @Before
    public void setUp() throws TekniskException, IkkeFunnetException {
        AbacContext abacContext = mock(AbacContext.class);
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());

        abacResponse = mock(XacmlResponse.class);

        AbacService abacService = mock(AbacService.class);
        when(abacService.evaluate(any())).thenReturn(abacResponse);

        pep = new PepImpl(abacService, abacContext);

        fagsakMocked = mock(Fagsak.class);
        behandlingMocked = mock(Behandling.class);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMocked);
        when(fagsakMocked.hentBruker()).thenReturn(new Aktoer());
        when(behandlingMocked.getFagsak()).thenReturn(fagsakMocked);

        tilgangService = new TilgangService(fagsakService, behandlingService, oppgaveService, pep);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testBehandlingsIdIkketilgang() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkTilgang(102323934);
    }

    @Test
    public void testBehandlingsIdOk() throws Exception {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkTilgang(102323934);
    }

    @Test
    public void sjekkRedigerbar_behandlingErRedigerbar_Ok() throws FunksjonellException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        when(behandlingMocked.erRedigerbar()).thenReturn(true);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkRedigerbarOgTilgang(123123123);
    }

    @Test(expected = FunksjonellException.class)
    public void sjekkRedigerbar_behandlingIkkeRedigerbar_girFeil() throws FunksjonellException, TekniskException {
        when(behandlingMocked.erRedigerbar()).thenReturn(false);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);

        tilgangService.sjekkRedigerbarOgTilgang(123123123);
    }

    @Test
    public void sjekkTilordnet_behandlingErTilordnetSaksbehandler_Ok() throws FunksjonellException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        Oppgave oppgave = new Oppgave.Builder().setTilordnetRessurs(saksbehandler).build();

        when(behandlingMocked.erRedigerbar()).thenReturn(true);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(any())).thenReturn(Optional.of(oppgave));

        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123);
    }

    @Test(expected = FunksjonellException.class)
    public void sjekkTilordnet_behandlingErIkkeTilordnetSaksbehandler_girFeil() throws FunksjonellException, TekniskException {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        Oppgave oppgave = new Oppgave.Builder().setTilordnetRessurs("Z000000").build();

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandlingMocked);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(any())).thenReturn(Optional.of(oppgave));

        tilgangService.sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(123123123);
    }

    @Test
    public void testFagsakOk() throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        tilgangService.sjekkSak(saksnummer);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testFagsakIkkeTilgang() throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        tilgangService.sjekkSak(saksnummer);
    }
}
