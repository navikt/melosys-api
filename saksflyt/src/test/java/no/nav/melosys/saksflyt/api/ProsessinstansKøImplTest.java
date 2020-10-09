package no.nav.melosys.saksflyt.api;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.impl.ProsessinstansKøImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProsessinstansKøImplTest {

    private ProsessinstansKø prosessinstansKø = new ProsessinstansKøImpl();

    @Test
    public void leggTil_prosessinstansEksisterer_forventFalse() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThat(prosessinstansKø.leggTil(prosessinstans)).isTrue();
        assertThat(prosessinstansKø.leggTil(prosessinstans)).isFalse();
    }

    @Test
    public void plukkNeste_enProsessinstansEksistererPlukkerToGanger_forventProsessinstansOgEmpty() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstansKø.leggTil(prosessinstans);

        assertThat(prosessinstansKø.plukkNeste()).hasValue(prosessinstans);
        assertThat(prosessinstansKø.plukkNeste()).isNotPresent();
    }

}