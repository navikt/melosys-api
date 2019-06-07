package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static no.nav.melosys.domain.ProsessSteg.JFR_AKTØR_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BingeImplTest {

    private BingeImpl binge = new BingeImpl();

    /*
     * Tester at vi kan legge til saker, hente dem og fjerne dem.
     */
    @Test
    public void testGrunnleggendeFunksjonalitet() {
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        ReflectionTestUtils.setField(pi1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(pi2, "id", UUID.randomUUID());
        pi1.setRegistrertDato(LocalDateTime.of(2017, 1, 2, 0, 0));
        pi2.setRegistrertDato(LocalDateTime.of(2017, 1, 1, 0, 0));
        pi1.setSteg(JFR_AKTØR_ID);
        pi2.setSteg(JFR_HENT_PERS_OPPL);
        assertTrue(binge.leggTil(pi1));
        assertTrue(binge.leggTil(pi2));
        assertFalse(binge.leggTil(pi2)); // Skal ikke kunne legge til samme sak flere ganger
        assertEquals(2, binge.hentProsessinstanser().size()); // Skal være 2 saker i bingen
        assertEquals(pi2, binge.hentOgSettProsessinstansTilAktiv((s) -> true)); // b2 har kortest frist
        assertEquals(pi1, binge.hentOgSettProsessinstansTilAktiv((s) -> true)); // Nå er det b1 som har kortest frist
        assertNull(binge.hentOgSettProsessinstansTilAktiv((s) -> true)); // Alle sakene er hentet ut. Fjern skal returnere null
    }

    @Test
    public void hentOgSettProsessinstansTilAktiv_Prosessinstans_erFremdelesIBinge() {
        Prosessinstans pi1 = new Prosessinstans();
        ReflectionTestUtils.setField(pi1, "id", UUID.randomUUID());
        pi1.setRegistrertDato(LocalDateTime.of(2017, 1, 2, 0, 0));
        binge.leggTil(pi1);

        binge.hentOgSettProsessinstansTilAktiv((s) -> true);
        assertThat(binge.hentProsessinstanser().size()).isEqualTo(1);
    }

    @Test
    public void fjernProsessinstans_Prosessinstans_erFjernetFraBinge() {
        Prosessinstans pi1 = new Prosessinstans();
        ReflectionTestUtils.setField(pi1, "id", UUID.randomUUID());
        pi1.setRegistrertDato(LocalDateTime.of(2017, 1, 2, 0, 0));
        binge.leggTil(pi1);
        binge.hentOgSettProsessinstansTilAktiv((s) -> true);

        binge.fjernFraAktiveProsessinstanser(pi1);
        assertThat(binge.hentProsessinstanser().size()).isEqualTo(0);
    }

    @Test
    public void leggTil_medStatusFeilet_funkerIkke() {
        Prosessinstans pi = new Prosessinstans();
        ReflectionTestUtils.setField(pi, "id", UUID.randomUUID());
        pi.setSteg(ProsessSteg.FEILET_MASKINELT);
        assertThat(binge.leggTil(pi)).isEqualTo(false);
    }

    @Test
    public void leggTil_medStatusFerdig_funkerIkke() {
        Prosessinstans pi = new Prosessinstans();
        ReflectionTestUtils.setField(pi, "id", UUID.randomUUID());
        pi.setSteg(ProsessSteg.FERDIG);
        assertThat(binge.leggTil(pi)).isEqualTo(false);
    }
}
