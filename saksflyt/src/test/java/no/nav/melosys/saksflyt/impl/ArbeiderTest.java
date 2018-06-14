package no.nav.melosys.saksflyt.impl;

import java.util.Arrays;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.jfr.HentPersonopplysninger;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ArbeiderTest {

    @Mock
    private HentPersonopplysninger klargjøreSteg;

    @Mock
    private Binge binge;
    
    @Mock
    private ProsessinstansRepository prosessinstansRepo;
    
    @Test
    public void testAtStegeneBlirKalt() throws Exception {
        MockitoAnnotations.initMocks(this);
        Arbeider arbeider = new Arbeider(binge, prosessinstansRepo, Arrays.asList(klargjøreSteg), 1, 15);
        when(binge.fjernFørsteProsessinstans(any())).thenReturn(new Prosessinstans());
        
        long medgåttTid = System.currentTimeMillis();
        arbeider.start();
        Thread.sleep(20);
        arbeider.stopp();
        medgåttTid = System.currentTimeMillis() - medgåttTid + 1;
        
        verify(klargjøreSteg, atLeast(21)).utførSteg(any());
        verify(klargjøreSteg, atMost(15 * (int) medgåttTid)).utførSteg(any());
    }

    @Test
    public void testLivssyklus() throws Exception {
        MockitoAnnotations.initMocks(this);
        Arbeider arbeider = new Arbeider(binge, prosessinstansRepo, Arrays.asList(klargjøreSteg), 1, 100);
        when(binge.fjernFørsteProsessinstans(any())).thenReturn(new Prosessinstans());
        ArbeiderTraad[] tråder = (ArbeiderTraad[]) ReflectionTestUtils.getField(arbeider, "tråder");

        arbeider.start();
        for (ArbeiderTraad tråd : tråder) {
            assertTrue(((Thread) tråd).isAlive());
        }
        arbeider.stopp();
        for (ArbeiderTraad tråd : tråder) {
            assertFalse(((Thread) tråd).isAlive());
        }
    }

}
