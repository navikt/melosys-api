package no.nav.melosys.saksflyt.impl.binge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.saksflyt.SaksflytApplication;
import no.nav.melosys.saksflyt.impl.steg.a1.KlargjoereSteg;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = { SaksflytApplication.class })
public class ArbeiderTest {

    @InjectMocks
    private Arbeider arbeider;

    @Mock
    private KlargjoereSteg klargjøreSteg;

    @Test
    public void testAtStegeneBlirKalt() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(arbeider, "antallTråder", 15);
        ReflectionTestUtils.setField(arbeider, "oppholdMellomSteg", 1);
        ReflectionTestUtils.setField(arbeider, "maskinelleSteg", Arrays.asList(klargjøreSteg));
        long medgåttTid = System.currentTimeMillis();
        arbeider.start();
        Thread.sleep(20);
        arbeider.stopp();
        medgåttTid = System.currentTimeMillis() - medgåttTid + 1;
        Mockito.verify(klargjøreSteg, atLeast(21)).finnBehandlingOgUtfoerSteg();
        Mockito.verify(klargjøreSteg, atMost(15 * (int) medgåttTid)).finnBehandlingOgUtfoerSteg();
    }

    @Test
    public void testLivssyklus() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(arbeider, "antallTråder", 100);
        ReflectionTestUtils.setField(arbeider, "oppholdMellomSteg", 1);
        ReflectionTestUtils.setField(arbeider, "maskinelleSteg", Arrays.asList(klargjøreSteg));
        arbeider.start();
        Object[] tråder = (Object[]) ReflectionTestUtils.getField(arbeider, "tråder");
        for (Object tråd : tråder) {
            assertTrue(((Thread) tråd).isAlive());
        }
        arbeider.stopp();
        for (Object tråd : tråder) {
            assertFalse(((Thread) tråd).isAlive());
        }
    }

}
