package no.nav.melosys.regler.motor.dekl;

import static no.nav.melosys.regler.motor.dekl.Predikat.ikke;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import no.nav.melosys.regler.motor.dekl.Predikat;

public class PredikatTest {

    @Test
    public void test() {
        Predikat alltidTrue = () -> {return true;};
        Predikat alltidFalse = ikke(alltidTrue);
        assertTrue(alltidTrue.test());
        assertFalse(alltidFalse.test());
        assertFalse(alltidTrue.og(alltidFalse).test());
        assertTrue(alltidTrue.eller(alltidFalse).test());
    }

}
