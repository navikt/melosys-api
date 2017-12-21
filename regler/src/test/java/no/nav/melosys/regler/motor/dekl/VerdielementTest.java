package no.nav.melosys.regler.motor.dekl;

import static no.nav.melosys.regler.motor.voc.Verdielement.antallet;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import no.nav.melosys.regler.motor.voc.Verdielement;

public class VerdielementTest {
    
    @Test
    public void testAntallet() {
        assertTrue(antallet(Collections.EMPTY_LIST).erLik(0).test());
        assertFalse(antallet(Collections.EMPTY_LIST).erLik(1).test());
        assertTrue(antallet(Arrays.asList(1, 2, 3, 4)).erLik(4).test());
    }

    @Test
    public void testManglerOgHarVerdi() {
        Verdielement tom = verdien(null);
        assertFalse(tom.harVerdi().test());
        assertTrue(tom.mangler().test());
        
    }
    
    @Test
    public void testErLik() {
        // Test primitive...
        assertTrue(verdien(4).erLik(4).test());
        assertFalse(verdien(5).erLik(4).test());
        // Test Boolean vs. bool...
        assertTrue(verdien(Boolean.FALSE).erLik(false).test());
        assertFalse(verdien(Boolean.TRUE).erLik(false).test());
        assertFalse(verdien(Boolean.FALSE).erLik(true).test());
        assertTrue(verdien(Boolean.TRUE).erLik(true).test());
        assertFalse(verdien(Boolean.FALSE).erSann().test());
        assertTrue(verdien(Boolean.TRUE).erSann().test());
        // Test referanselikhet...
        assertFalse(verdien(new Object()).erLik(new Object()).test());
    }
    
    @Test
    public void testSammenlikning() {
        assertTrue(verdien(7).erStørreEnnEllerLik(4).test());
        assertFalse(verdien(7).erStørreEnn(7).test());
    }
    
}
