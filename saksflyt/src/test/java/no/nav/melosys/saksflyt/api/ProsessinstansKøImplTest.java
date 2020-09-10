package no.nav.melosys.saksflyt.api;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.impl.ProsessinstansKøImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProsessinstansKøImplTest {

    private ProsessinstansKø binge = new ProsessinstansKøImpl();

    @Test
    public void leggTil_prosessinstansEksisterer_forventFalse() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThat(binge.leggTil(prosessinstans)).isTrue();
        assertThat(binge.leggTil(prosessinstans)).isFalse();
    }

    @Test
    public void plukkNeste_enProsessinstansEksistererPlukkerToGanger_forventProsessinstansOgEmpty() {
        Prosessinstans prosessinstans = new Prosessinstans();
        binge.leggTil(prosessinstans);

        assertThat(binge.plukkNeste()).hasValue(prosessinstans);
        assertThat(binge.plukkNeste()).isNotPresent();
    }

}