package no.nav.melosys.service;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingServiceTest {

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepo;

    private BehandlingService behandlingService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepo, behandlingsresultatRepository, tidligereMedlemsperiodeRepo);
    }

    @Test
    public void oppdaterStatus() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = IkkeFunnetException.class)
    public void oppdaterStatus_behIkkeFunnet() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findOne(anyLong())).thenReturn(null);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = FunksjonellException.class)
    public void oppdaterStatus_ugyldig() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVSLUTTET);
    }

    @Test
    public void knyttMedlemsperioder_ingenBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findOne(anyLong())).thenReturn(null);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandling " + behandlingID + " finnes ikke.");

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder_avsluttetBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
        verify(tidligereMedlemsperiodeRepo, times(1)).deleteById_BehandlingId(behandlingID);
        verify(tidligereMedlemsperiodeRepo, times(1)).save(anyList());
    }

    @Test
    public void finnMedlemsperioder_ingenTidligereMedlemsperioder() {
        long behandlingID = 11L;
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(null);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).isEmpty();
    }

    @Test
    public void hentMedlemsperioder() {
        long behandlingID = 11L;
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = Arrays.asList(
            new TidligereMedlemsperiode(behandlingID, 2L),
            new TidligereMedlemsperiode(behandlingID, 3L));
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(tidligereMedlemsperioder);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).containsExactly(2L, 3L);
    }

    @Test
    public void nyBehandling() {
        Behandling behandling = behandlingService.nyBehandling(new Fagsak(), Behandlingsstatus.OPPRETTET, Behandlingstype.SØKNAD);
        verify(behandlingRepo).save(any(Behandling.class));
        assertThat(behandling.getType()).isEqualTo(Behandlingstype.SØKNAD);
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
    }
}