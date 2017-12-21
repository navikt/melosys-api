package no.nav.melosys.regler.motor.dekl;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import no.nav.melosys.regler.motor.voc.Deklarasjon;

public class DeklarasjonTest {

    boolean såDelUtført = false;

    @Test
    public void testDekl() {
        Deklarasjon.hvis(() -> true)
        .så(() -> såDelUtført = true)
        .ellers(Assert::fail);
        assertTrue(såDelUtført);
    }
    
}
