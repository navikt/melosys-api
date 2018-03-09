package no.nav.melosys.saksflyt.impl.domain;

import static no.nav.melosys.domain.ProsessSteg.KLARGJORT;
import static no.nav.melosys.domain.ProsessSteg.NY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.saksflyt.impl.Utils;

public class BehandlingUtilsTest {

    @Test
    public void testPredicateMedStatus() {
        Behandling b = new Behandling();
        b.setSteg(NY);
        assertFalse(Utils.medSteg(KLARGJORT).test(b));
        b.setSteg(ProsessSteg.KLARGJORT);
        assertTrue(Utils.medSteg(KLARGJORT).test(b));
    }

    @Test
    public void testComparatorElsdteFørst() {
        Behandling s1 = new Behandling(), s2 = new Behandling();
        s1.setRegistrertDato(LocalDateTime.of(2017, 01, 01, 0, 0, 0));
        s2.setRegistrertDato(LocalDateTime.of(2017, 01, 02, 0, 0, 0));
        assertTrue(Utils.eldsteFørst().compare(s1, s1) == 0);
        assertTrue(Utils.eldsteFørst().compare(s1, s2) < 0);
        assertTrue(Utils.eldsteFørst().compare(s2, s1) > 0);
    }

}
