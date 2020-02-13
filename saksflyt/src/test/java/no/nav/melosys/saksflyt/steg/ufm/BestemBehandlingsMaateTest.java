package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BestemBehandlingsMaateTest {

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BestemBehandlingsMaate bestemBehandlingsMaate;

    @Before
    public void setUp() {
        bestemBehandlingsMaate = new BestemBehandlingsMaate(behandlingsresultatRepository);
    }

    @Test
    public void utførSteg_ingenTreffIRegister_verifiserNesteSteg() throws Exception {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        when(behandlingsresultatRepository.findWithKontrollresultaterById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        Prosessinstans prosessinstans = hentProsessinstans();
        bestemBehandlingsMaate.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
    }

    @Test
    public void utførSteg_treffIRegister_verifiserNesteSteg() throws Exception {
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(Kontroll_begrunnelser.FEIL_I_PERIODEN);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setKontrollresultater(Set.of(kontrollresultat));
        when(behandlingsresultatRepository.findWithKontrollresultaterById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

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