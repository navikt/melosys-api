package no.nav.melosys.saksflyt.impl;

import java.util.Arrays;

import no.nav.melosys.saksflyt.SaksflytTestApplication;
import no.nav.melosys.saksflyt.agent.jfr.HentPersonopplysninger;
import no.nav.melosys.saksflyt.impl.Arbeider;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.ArgumentMatchers.any;


@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = { SaksflytTestApplication.class })
public class ArbeiderTest {

    @InjectMocks
    private Arbeider arbeider;

    @Mock
    private HentPersonopplysninger klargjøreSteg;

    @Test
    @Ignore // FIXME
    public void testAtStegeneBlirKalt() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(arbeider, "antallTråder", 15);
        ReflectionTestUtils.setField(arbeider, "oppholdMellomSteg", 1);
        ReflectionTestUtils.setField(arbeider, "agenter", Arrays.asList(klargjøreSteg));
        long medgåttTid = System.currentTimeMillis();
        arbeider.start();
        Thread.sleep(20);
        arbeider.stopp();
        medgåttTid = System.currentTimeMillis() - medgåttTid + 1;
        Mockito.verify(klargjøreSteg, atLeast(21)).utførSteg(any());
        Mockito.verify(klargjøreSteg, atMost(15 * (int) medgåttTid)).utførSteg(any());
    }

    @Test
    @Ignore // FIXME
    public void testLivssyklus() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(arbeider, "antallTråder", 100);
        ArbeiderTraad[] tråder = (ArbeiderTraad[]) ReflectionTestUtils.getField(arbeider, "tråder");
        for (ArbeiderTraad tråd : tråder) {
            ReflectionTestUtils.setField(tråd, "oppholdMellomSteg", 1);
            ReflectionTestUtils.setField(tråd, "agenter", Arrays.asList(klargjøreSteg));
        }
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
