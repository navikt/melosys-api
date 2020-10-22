package no.nav.melosys.saksflyt.kontroll;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.ProsessinstansKøImpl;
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
    private ProsessinstansKøImpl kø;

    private SaksflytKontroll saksflytKontroll;

    private final static int TOTAL_PROSESSINSTANSER = 10;

    private List<Prosessinstans> prosessinstanser;

    @Before
    public void setup() {
        saksflytKontroll = new SaksflytKontroll(kø, prosessinstansRepository);
        prosessinstanser = lagProsessInstanser();
        when(prosessinstansRepository.findAllByStatus(eq(ProsessStatus.KLAR))).thenReturn(prosessinstanser);
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_1ProsessinstansIKø1Endret1MinSiden_8ProsessinstanserBlirLagtTil() {

        kø.leggTil(prosessinstanser.get(0));
        when(prosessinstanser.get(1).getEndretDato()).thenReturn(LocalDateTime.now());

        for (int i = 2; i < TOTAL_PROSESSINSTANSER; i++) {
            when(prosessinstanser.get(i).getEndretDato()).thenReturn(LocalDateTime.now().minusMinutes(16));
        }

        clearInvocations(kø);
        saksflytKontroll.sjekkProsessinstansFinnesISaksflyt();
        verify(kø, times(8)).leggTil(any(Prosessinstans.class));
    }

    @Test
    public void sjekkProsessinstansFinnesISaksFlyt_ingenSkalLeggesTil_bingeSinStørrelseIkkeEndret() {
        prosessinstanser.forEach(kø::leggTil);
        clearInvocations(kø);

        saksflytKontroll.sjekkProsessinstansFinnesISaksflyt();
        verify(kø, times(0)).leggTil(any(Prosessinstans.class));
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