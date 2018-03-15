package no.nav.melosys.saksflyt.impl.domain;

import static no.nav.melosys.domain.ProsessSteg.A1_HENT_PERS_OPPL;
import static no.nav.melosys.domain.ProsessSteg.A1_JOURF;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.impl.Utils;

public class BehandlingUtilsTest {

    @Test
    public void testPredicateMedStatus() {
        Prosessinstans pi = new Prosessinstans();
        pi.setSteg(A1_JOURF);
        assertFalse(Utils.medSteg(A1_HENT_PERS_OPPL).test(pi));
        pi.setSteg(ProsessSteg.A1_HENT_PERS_OPPL);
        assertTrue(Utils.medSteg(A1_HENT_PERS_OPPL).test(pi));
    }

    @Test
    public void testComparatorElsdteFørst() {
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setRegistrertDato(LocalDateTime.of(2017, 01, 01, 0, 0, 0));
        pi2.setRegistrertDato(LocalDateTime.of(2017, 01, 02, 0, 0, 0));
        assertTrue(Utils.eldsteFørst().compare(pi1, pi1) == 0);
        assertTrue(Utils.eldsteFørst().compare(pi1, pi2) < 0);
        assertTrue(Utils.eldsteFørst().compare(pi2, pi1) > 0);
    }

}
