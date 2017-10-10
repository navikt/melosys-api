package no.nav.melosys.saksflyt.impl.binge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.saksflyt.impl.Utils;

public class BingeImplTest {

    /*
     * Tester at vi kan legge til saker, hente dem og fjerne dem.
     */
    @Test
    public void testGrunnleggendeFunksjonalitet() {
        BingeImpl binge = new BingeImpl();
        Behandling b1 = new Behandling(), b2 = new Behandling();
        ReflectionTestUtils.setField(b1, "id", 1L);
        ReflectionTestUtils.setField(b2, "id", 2L);
        b1.setRegistrertDato(LocalDateTime.of(2017, 1, 2, 0, 0));
        b2.setRegistrertDato(LocalDateTime.of(2017, 1, 1, 0, 0));
        b1.setStatus(BehandlingStatus.OPPRETTET);
        b2.setStatus(BehandlingStatus.UNDER_BEHANDLING);
        assertTrue(binge.leggTil(b1));
        assertTrue(binge.leggTil(b2));
        assertFalse(binge.leggTil(b2)); // Skal ikke kunne legge til samme sak flere ganger
        assertNull(binge.hentBehandling(0)); // Ingen sak med saksId 0 er lagt inn
        assertEquals(2, binge.hentBehandlinger((s) -> true).size()); // Skal være 2 saker i bingen
        assertEquals(0, binge.hentBehandlinger((s) -> false).size()); // Skal ikke returnere noe hvis predikatet ikke slår til
        assertEquals(b2, binge.fjernFørsteBehandling((s) -> true, Utils.eldsteFørst())); // b2 har kortest frist
        assertEquals(b1, binge.fjernFørsteBehandling((s) -> true, Utils.eldsteFørst())); // Nå er det b1 som har kortest frist
        assertNull(binge.fjernFørsteBehandling((s) -> true, Utils.eldsteFørst())); // Alle sakene er hentet ut. Fjern skal returnere null
    }

}
