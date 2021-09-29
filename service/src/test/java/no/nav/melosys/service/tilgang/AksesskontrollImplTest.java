package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AksesskontrollImplTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private TilgangService tilgangService;
    @Mock
    private RedigerbarKontroll redigerbarKontroll;

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

        aksesskontroll = new AksesskontrollImpl(fagsakService, behandlingService, tilgangService, redigerbarKontroll);
    }

    @Test
    void autoriserSakstilgang_sjekkerBruker() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        aksesskontroll.autoriserSakstilgang(saksnummer);
        verify(tilgangService).validerTilgangTilAktørID(aktørID);
    }

    @Test
    void autoriser_verifiserSjekkLesetilgang() {
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        aksesskontroll.autoriser(behandlingID);
        verify(tilgangService).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll, never()).sjekkRessursRedigerbar(behandling, Ressurs.UKJENT);
    }

    @Test
    void autoriser_skalSkrive_verifiserRedigerbarBehandling() {
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        aksesskontroll.autoriser(behandlingID, Aksesstype.SKRIV);
        verify(tilgangService).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll).sjekkRessursRedigerbar(behandling, Ressurs.UKJENT);
    }

    @Test
    void autoriserSkrivTilRessurs_verifiserRedigerbarBehandlingSjekkes() {
        final var skrivTilRessurs = Ressurs.AVKLARTE_FAKTA;
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, skrivTilRessurs);
        verify(tilgangService).validerTilgangTilAktørID(aktørID);
        verify(redigerbarKontroll).sjekkRessursRedigerbar(behandling, skrivTilRessurs);
    }
}
