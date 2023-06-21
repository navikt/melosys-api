package no.nav.melosys.service.tilgang;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.sikkerhet.logging.AuditEvent;
import no.nav.melosys.sikkerhet.logging.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AksesskontrollImplTest {

    @Spy
    private AuditLogger auditLogger = new AuditLogger();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BrukertilgangKontroll brukertilgangKontroll;
    @Mock
    private RedigerbarKontroll redigerbarKontroll;
    @Mock
    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<AuditEvent> auditCaptor;

    private Aksesskontroll aksesskontroll;

    private final Fagsak fagsak = new Fagsak();
    private final Behandling behandling = new Behandling();
    private final String aktørID = "412423";
    private final String saksnummer = "MEL-0";
    private final long behandlingID = 1111;

    @BeforeEach
    void setup() {
        behandling.setId(behandlingID);
        behandling.setFagsak(fagsak);
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(aktørID);
        fagsak.getAktører().add(aktoer);
        fagsak.setSaksnummer(saksnummer);

        SubjectHandler.set(new TestSubjectHandler());

        aksesskontroll = new AksesskontrollImpl(auditLogger, fagsakService, behandlingService, brukertilgangKontroll, redigerbarKontroll, oppgaveService);
    }

    @Test
    void auditAutoriserFolkeregisterIdent_auditOgSjekkTilgang() {
        aksesskontroll.auditAutoriserFolkeregisterIdent("fnr", "melding");

        verify(auditLogger).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getSourceUserId()).isNotNull();
        assertThat(auditCaptor.getValue().getDestinationUserId()).isEqualTo("fnr");
        assertThat(auditCaptor.getValue().getMessage()).isEqualTo("melding");
        verify(brukertilgangKontroll).validerTilgangTilFolkeregisterIdent("fnr");
    }

    @Test
    void auditAutoriserSakstilgang_audigOgSjekkTilgang() {
        aksesskontroll.auditAutoriserSakstilgang(fagsak, "melding");

        verify(auditLogger).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getSourceUserId()).isNotNull();
        assertThat(auditCaptor.getValue().getDestinationUserId()).isEqualTo(fagsak.finnBrukersAktørID().get());
        assertThat(auditCaptor.getValue().getMessage()).isEqualTo("melding");
        verify(brukertilgangKontroll).validerTilgangTilAktørID(fagsak.finnBrukersAktørID().get());
    }

    @Test
    void autoriserSakstilgang_sjekkerBruker() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        aksesskontroll.autoriserSakstilgang(saksnummer);

        verify(brukertilgangKontroll).validerTilgangTilAktørID(aktørID);
    }

    @Test
    void autoriser_verifiserSjekkLesetilgang() {
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        aksesskontroll.autoriser(behandlingID);

        verify(brukertilgangKontroll).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll, never()).sjekkRessursRedigerbar(behandling, Ressurs.UKJENT);
    }

    @Test
    void autoriser_skalSkrive_verifiserRedigerbarBehandling() {
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        aksesskontroll.autoriser(behandlingID, Aksesstype.SKRIV);

        verify(brukertilgangKontroll).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll).sjekkRessursRedigerbar(behandling, Ressurs.UKJENT);
    }

    @Test
    void autoriser_harIkkeBruker_verifiserIkkeSjekkAktørID() {
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        behandling.getFagsak().setAktører(Set.of(virksomhet));
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        aksesskontroll.autoriser(behandlingID, Aksesstype.SKRIV);

        verify(brukertilgangKontroll, never()).validerTilgangTilAktørID(any());
        verify(redigerbarKontroll).sjekkRessursRedigerbar(behandling, Ressurs.UKJENT);
    }

    @Test
    void autoriserSkrivTilRessurs_verifiserRedigerbarBehandlingSjekkes() {
        final var skrivTilRessurs = Ressurs.AVKLARTE_FAKTA;
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, skrivTilRessurs);

        verify(brukertilgangKontroll).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll).sjekkRessursRedigerbar(behandling, skrivTilRessurs);
    }

    @Test
    void behandlingKanRedigeresAvSaksbehandler_behandlingIkkeRedigerbar_ikkeSann() {
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);

        var saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, "Z123");

        assertThat(saksbehandlerHarTilgang).isFalse();
    }

    @Test
    void behandlingKanRedigeresAvSaksbehandler_behandlingRedigerbarOppgaveIkkeTilordnet_ikkeSann() {
        final var saksbehandler = "Z111111";
        when(redigerbarKontroll.behandlingErRedigerbar(behandling)).thenReturn(true);
        when(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).thenReturn(false);

        var saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler);

        assertThat(saksbehandlerHarTilgang).isFalse();
    }

    @Test
    void behandlingKanRedigeresAvSaksbehandler_behandlingRedigerbarOppgaveTilordnet_sann() {
        final var saksbehandler = "Z111111";
        when(redigerbarKontroll.behandlingErRedigerbar(behandling)).thenReturn(true);
        when(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).thenReturn(true);

        var saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler);

        assertThat(saksbehandlerHarTilgang).isTrue();
    }
}
