package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AksesskontrollImplTest {

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

        aksesskontroll = new AksesskontrollImpl(fagsakService, behandlingService, brukertilgangKontroll, redigerbarKontroll, oppgaveService);
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
        assertThat(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, "Z123")).isFalse();
    }

    @Test
    void behandlingKanRedigeresAvSaksbehandler_behandlingRedigerbarOppgaveIkkeTilordnet_ikkeSann() {
        final var saksbehandler = "Z111111";
        when(redigerbarKontroll.behandlingErRedigerbar(behandling)).thenReturn(true);
        when(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).thenReturn(false);
        assertThat(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler)).isFalse();
    }

    @Test
    void behandlingKanRedigeresAvSaksbehandler_behandlingRedigerbarOppgaveTilordnet_sann() {
        final var saksbehandler = "Z111111";
        when(redigerbarKontroll.behandlingErRedigerbar(behandling)).thenReturn(true);
        when(oppgaveService.saksbehandlerErTilordnetOppgaveForSaksnummer(saksbehandler, saksnummer)).thenReturn(true);
        assertThat(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler)).isTrue();
    }
}
