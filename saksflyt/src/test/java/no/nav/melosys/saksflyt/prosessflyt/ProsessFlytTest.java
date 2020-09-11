package no.nav.melosys.saksflyt.prosessflyt;


import java.util.List;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class ProsessFlytTest {

    private ProsessFlyt prosessFlyt;
    private final List<ProsessSteg> stegListe = List.of(
        ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE,
        ProsessSteg.MSA_OPPRETT_ARKIVSAK,
        ProsessSteg.VS_SEND_SOKNAD
    );

    @Before
    public void setup() {
        prosessFlyt = new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK, stegListe);
    }

    @Test
    public void nesteSteg_forrigeStegErNull_forventFørsteElement() {
        assertThat(prosessFlyt.nesteSteg(null)).isEqualTo(stegListe.get(0));
    }

    @Test
    public void nesteSteg_forrigeStegErFørsteElement_forventAndreElement() {
        assertThat(prosessFlyt.nesteSteg(stegListe.get(0))).isEqualTo(stegListe.get(1));
    }

    @Test
    public void nesteSteg_forrigeStegErSisteElement_forventNull() {
        assertThat(prosessFlyt.nesteSteg(stegListe.get(stegListe.size() - 1))).isNull();
    }

    @Test
    public void nesteSteg_forrigeStegIkkeEnDelAvFlyt_kasterException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> prosessFlyt.nesteSteg(ProsessSteg.AFL_AVSLUTT_TIDLIGERE_PERIODE))
            .withMessageContaining("ikke gyldig for prosesstype");
    }
}