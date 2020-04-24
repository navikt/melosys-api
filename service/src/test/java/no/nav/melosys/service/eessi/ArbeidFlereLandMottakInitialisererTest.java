package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArbeidFlereLandMottakInitialisererTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private ArbeidFlereLandMottakInitialiserer arbeidFlereLandMottakInitialiserer;

    private final long behandlingID = 123;
    private final Long gsakSaksnummer = 1111L;

    private Behandling behandling;
    private Fagsak fagsak;

    @Before
    public void setup() {
        arbeidFlereLandMottakInitialiserer = new ArbeidFlereLandMottakInitialiserer(fagsakService, behandlingService, oppgaveService);

        behandling = new Behandling();
        behandling.setId(behandlingID);
        fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
    }

    @Test
    public void finnsakOgBestemRuting_fagsakEksistererIkke_forventNySakRuting() throws FunksjonellException, TekniskException {
        RutingResultat rutingResultat = arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), null);
        assertThat(rutingResultat).isEqualTo(RutingResultat.NY_SAK);
    }

    @Test
    public void finnsakOgBestemRuting_fagsakEksistererBehandlingAktiv_forventIngenBehandlingBehandlingsstautsOppdateres() throws FunksjonellException, TekniskException {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        RutingResultat rutingResultat = arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(rutingResultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        verify(behandlingService).oppdaterStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    public void finnsakOgBestemRuting_fagsakEksistererBehandlingAvsluttet_forventIngenBehandlingOppgaveOpprettes() throws FunksjonellException, TekniskException {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);

        RutingResultat rutingResultat = arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(rutingResultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any());
    }
}