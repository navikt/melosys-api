package no.nav.melosys.saksflyt.impl.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.saksflyt.impl.Utils;

public class BehandlingUtilsTest {

    @Test
    public void testPredicateMedStatus() {
        Behandling b = new Behandling();
        b.setStatus(BehandlingStatus.OPPRETTET);
        assertFalse(Utils.medStatus(BehandlingStatus.KLARGJORT).test(b));
        b.setStatus(BehandlingStatus.OPPRETTET);
        assertFalse(Utils.medStatus(BehandlingStatus.KLARGJORT).test(b));
        b.setStatus(BehandlingStatus.KLARGJORT);
        assertTrue(Utils.medStatus(BehandlingStatus.KLARGJORT).test(b));
    }

    /* FIXME
    @Test
    public void testComparatorKortestFristFørst() {
        Behandling s1 = new Behandling(), s2 = new Behandling();
        s1.setFrist(LocalDate.of(2017, 01, 01));
        s2.setFrist(LocalDate.of(2017, 01, 02));
        assertTrue(Utils.kortestFristFørst().compare(s1, s1) == 0);
        assertTrue(Utils.kortestFristFørst().compare(s1, s2) < 0);
        assertTrue(Utils.kortestFristFørst().compare(s2, s1) > 0);
    }
    //*/

}
