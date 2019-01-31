package no.nav.melosys.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsresultatServiceTest {

    private BehandlingsresultatRepository behandlingsresultatRepo;

    private BehandlingsresultatService behandlingsresultatService;

    @Before
    public void setUp() {
        behandlingsresultatRepo = mock(BehandlingsresultatRepository.class);
        behandlingsresultatService = new BehandlingsresultatService(behandlingsresultatRepo);
    }

    @Test
    public void tømBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(new HashSet<>(Collections.singletonList(new Avklartefakta())));
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(Collections.singletonList(new Lovvalgsperiode())));
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Collections.singleton(new Vilkaarsresultat())));

        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatService.tømBehandlingsresultat(1L);

        assertThat(behandlingsresultat.getAvklartefakta()).isEmpty();
        assertThat(behandlingsresultat.getLovvalgsperioder()).isEmpty();
        assertThat(behandlingsresultat.getVilkaarsresultater()).isEmpty();
    }

    @Test(expected = IkkeFunnetException.class)
    public void hentBehandlingsresultat_medTomtResultat_forventerException() throws IkkeFunnetException {
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.empty());
        behandlingsresultatService.hentBehandlingsresultat(4L);
    }

    @Test
    public void hentBehandlingsresultat_returnererBehandlingsresultat() throws IkkeFunnetException {
        Behandlingsresultat resultat = new Behandlingsresultat();
        resultat.setHenleggelsesgrunn(Henleggelsesgrunner.ANNET);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(resultat));

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(4L);
        assertThat(behandlingsresultat.getHenleggelsesgrunn()).isEqualTo(Henleggelsesgrunner.ANNET);
    }
}