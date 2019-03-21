package no.nav.melosys.saksflyt.kontroll;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.impl.BingeImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksflytSchedulerTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    private Binge binge = new BingeImpl();

    private SaksflytScheduler saksflytScheduler;

    private final static int TOTAL_PROSESSINSTANSER = 10;

    private List<Prosessinstans> prosessinstanser;

    @Before
    public void setup() {
        saksflytScheduler = new SaksflytScheduler(binge, prosessinstansRepository);
        prosessinstanser = prosessInstanser();
        when(prosessinstansRepository.findAllByStegIsNotNullAndStegIsNot(any(ProsessSteg.class))).thenReturn(prosessinstanser);
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_leggTilProsessinstanserSomIkkeFinnes() {

        int prosesinstanserIBinge = 5;
        for (int i = 0; i < prosesinstanserIBinge; i++) {
            binge.leggTil(prosessinstanser.get(i));
        }
        assertThat(binge.hentProsessinstanser().size()).isEqualTo(prosesinstanserIBinge);

        saksflytScheduler.sjekkProsessinstansFinnesISaksflyt();

        assertThat(binge.hentProsessinstanser().size()).isEqualTo(TOTAL_PROSESSINSTANSER);
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_ingenSkalLeggesTil_bingeSinStørrelseIkkeEndret() {
        prosessinstanser.forEach(binge::leggTil);

        int nåværendeStørrelse = binge.hentProsessinstanser().size();
        assertThat(nåværendeStørrelse).isEqualTo(TOTAL_PROSESSINSTANSER);

        saksflytScheduler.sjekkProsessinstansFinnesISaksflyt();
        assertThat(binge.hentProsessinstanser().size()).isEqualTo(TOTAL_PROSESSINSTANSER);
    }

    private List<Prosessinstans> prosessInstanser() {
        List<Prosessinstans> prosessinstanser = new ArrayList<>();
        for (int i = 0; i < TOTAL_PROSESSINSTANSER; i++) {
            Prosessinstans prosessinstans = mock(Prosessinstans.class);
            when(prosessinstans.getId()).thenReturn(UUID.randomUUID());
            prosessinstanser.add(prosessinstans);
        }

        return prosessinstanser;
    }

}