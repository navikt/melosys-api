package no.nav.melosys.saksflyt.kontroll;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.BingeImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SaksflytKontrollTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Spy
    private BingeImpl binge;

    private SaksflytKontroll saksflytKontroll;

    private final static int TOTAL_PROSESSINSTANSER = 10;

    private List<Prosessinstans> prosessinstanser;

    @Before
    public void setup() {
        saksflytKontroll = new SaksflytKontroll(binge, prosessinstansRepository);
        prosessinstanser = lagProsessInstanser();
        when(prosessinstansRepository.findAllByStegIsNotAndStegIsNot(any(ProsessSteg.class),any(ProsessSteg.class))).thenReturn(prosessinstanser);
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_leggTilProsessinstanserSomIkkeFinnes() {

        for (int i = 0; i < 5; i++) {
            binge.leggTil(prosessinstanser.get(i));
        }
        clearInvocations(binge);

        saksflytKontroll.sjekkProsessinstansFinnesISaksflyt();
        verify(binge, times(5)).leggTil(any(Prosessinstans.class));
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_ingenSkalLeggesTil_bingeSinStørrelseIkkeEndret() {
        prosessinstanser.forEach(binge::leggTil);
        clearInvocations(binge);

        saksflytKontroll.sjekkProsessinstansFinnesISaksflyt();
        verify(binge, times(0)).leggTil(any(Prosessinstans.class));
    }

    private List<Prosessinstans> lagProsessInstanser() {
        List<Prosessinstans> prosessinstanser = new ArrayList<>();
        for (int i = 0; i < TOTAL_PROSESSINSTANSER; i++) {
            Prosessinstans prosessinstans = mock(Prosessinstans.class);
            when(prosessinstans.getId()).thenReturn(UUID.randomUUID());
            prosessinstanser.add(prosessinstans);
        }

        return prosessinstanser;
    }

}