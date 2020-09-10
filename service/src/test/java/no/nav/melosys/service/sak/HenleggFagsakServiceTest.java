package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HenleggFagsakServiceTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private HenleggFagsakService henleggFagsakService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Fagsak fagsak = new Fagsak();
    private final Behandling behandling = new Behandling();

    private final String saksnummer = "MEL-0";
    private final long behandlingID = 11;

    @Before
    public void setup() throws IkkeFunnetException {
        henleggFagsakService = new HenleggFagsakService(fagsakService, behandlingsresultatService, prosessinstansService, oppgaveService);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        fagsak.setSaksnummer(saksnummer);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterSisteBehandling() throws TekniskException, FunksjonellException {
        String fritekst = "Fri tale";
        henleggFagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansFagsakHenlagt(eq(behandling));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(fagsak.getSaksnummer()));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(Behandlingsresultattyper.HENLEGGELSE, fritekst);
    }

    @Test
    public void henleggFagsak_ikkeGyldigHenleggelsesgrunn_kasterException() {
        String saksnummer = "123456789";

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> henleggFagsakService.henleggFagsak(saksnummer, "UGYLDIGKODE", "Fri tale"))
            .withMessageContaining("ingen gyldig henleggelsesgrunn");
    }
}