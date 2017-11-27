package no.nav.melosys.regler.motor.dekl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;

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
