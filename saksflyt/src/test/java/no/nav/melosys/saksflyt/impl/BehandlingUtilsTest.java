package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;

import org.junit.Test;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.impl.Utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static no.nav.melosys.domain.ProsessSteg.JFR_AKTOER_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;

public class BehandlingUtilsTest {

    @Test
    public void testPredicateMedStatus() {
        Prosessinstans pi = new Prosessinstans();
        pi.setSteg(JFR_AKTOER_ID);
        assertFalse(Utils.medSteg(JFR_HENT_PERS_OPPL).test(pi));
        pi.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);
        assertTrue(Utils.medSteg(JFR_HENT_PERS_OPPL).test(pi));
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
