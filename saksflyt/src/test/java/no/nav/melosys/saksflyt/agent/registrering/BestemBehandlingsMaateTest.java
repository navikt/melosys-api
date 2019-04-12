package no.nav.melosys.saksflyt.agent.registrering;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BestemBehandlingsMaateTest {

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    private BestemBehandlingsMaate bestemBehandlingsMaate;

    @Before
    public void setUp() {
        bestemBehandlingsMaate = new BestemBehandlingsMaate(behandlingsresultatRepository, avklarteFaktaRepository);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void utførSteg_ingenTreffIRegister_verifiserNesteSteg() throws Exception {
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatype.VURDERING_UNNTAK_PERIODE)))
            .thenReturn(Sets.newSet());

        Prosessinstans prosessinstans = hentProsessinstans();
        bestemBehandlingsMaate.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
    }

    @Test
    public void utførSteg_treffIRegister_verifiserNesteSteg() throws Exception {
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatype.VURDERING_UNNTAK_PERIODE)))
            .thenReturn(Sets.newSet(new Avklartefakta()));

        Prosessinstans prosessinstans = hentProsessinstans();
        bestemBehandlingsMaate.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }
}