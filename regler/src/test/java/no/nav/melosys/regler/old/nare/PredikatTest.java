package no.nav.melosys.regler.nare;

import static no.nav.melosys.regler.nare.Predikat.ikke;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
