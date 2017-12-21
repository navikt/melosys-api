package no.nav.melosys.regler.dummy.dumdum; // Må være utenfor no.nav.melosys.regler.motor, ellers klager RegelLogg

import static no.nav.melosys.regler.motor.KontekstManager.initialiserLokalKontekst;
import static no.nav.melosys.regler.motor.KontekstManager.settVariabel;
import static no.nav.melosys.regler.motor.KontekstManager.slettLokalKontekst;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import no.nav.melosys.regler.motor.KontekstManager;
import no.nav.melosys.regler.motor.Regelflyt;
import no.nav.melosys.regler.motor.Regelpakke;

public class TestMotor extends Regelflyt implements Regelpakke {

    @Test
    public void testMotor() {
        initialiserLokalKontekst();
        TestMotor testMotor = new TestMotor();
        testMotor.leggTilRegelpakker(TestMotor.class);
        testMotor.kjør();
        assertEquals(6, KontekstManager.hentVariabel("seks"));
        slettLokalKontekst();
    }

    @Regel
    public static void enRegel() {
        settVariabel("seks", 6);
    }
    
}
