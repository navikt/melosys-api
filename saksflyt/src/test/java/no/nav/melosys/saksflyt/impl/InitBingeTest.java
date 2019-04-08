package no.nav.melosys.saksflyt.impl;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class InitBingeTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    private InitBinge initBinge;

    @Before
    public void setUp() {
        initBinge = new InitBinge(binge, prosessinstansRepo);
    }

    @Test
    public void afterPropertiesSet() {
        List<Prosessinstans> testBehandlinger = new ArrayList<>();

        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();

        testBehandlinger.add(pi1);
        testBehandlinger.add(pi2);

        when(prosessinstansRepo.findAllByStegIsNotAndStegIsNot(eq(ProsessSteg.FERDIG), eq(ProsessSteg.FEILET_MASKINELT))).thenReturn(testBehandlinger);
        when(binge.leggTil(any(Prosessinstans.class))).thenReturn(true);

        initBinge.afterPropertiesSet();

        verify(binge, times(testBehandlinger.size())).leggTil(any(Prosessinstans.class));
    }

}