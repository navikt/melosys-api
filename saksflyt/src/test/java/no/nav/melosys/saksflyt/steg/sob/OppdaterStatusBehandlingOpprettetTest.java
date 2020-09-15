package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sob.SobService;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OppdaterStatusBehandlingOpprettetTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SobService sobService;

    private OppdaterStatusBehandlingOpprettet oppdaterStatusBehandlingOpprettet;

    @BeforeEach
    public void setUp() {
        oppdaterStatusBehandlingOpprettet = new OppdaterStatusBehandlingOpprettet(sobService);
    }

    @Test
    void utfør_alltid_kallerSakOgBehandling() throws TekniskException, FunksjonellException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setId(123L);

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("123");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterStatusBehandlingOpprettet.utfør(prosessinstans);

        verify(sobService).sakOgBehandlingOpprettet(eq(fagsak.getSaksnummer()), eq(behandling.getId()), eq(bruker.getAktørId()));
    }
}