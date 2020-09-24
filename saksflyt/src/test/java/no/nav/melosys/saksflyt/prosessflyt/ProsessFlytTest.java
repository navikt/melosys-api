package no.nav.melosys.saksflyt.prosessflyt;


import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ProsessFlytTest {

    private final ProsessSteg førsteSteg = ProsessSteg.OPPRETT_AVGIFTSOPPGAVE;
    private final ProsessSteg andreSteg = ProsessSteg.OPPRETT_ARKIVSAK;
    private final ProsessSteg sisteSteg = ProsessSteg.VS_SEND_SOKNAD;

    @Test
    void nesteSteg_forrigeStegErNull_forventFørsteElement() {
        ProsessFlyt prosessFlyt = lagProsessFlyt();
        assertThat(prosessFlyt.nesteSteg(null)).isEqualTo(førsteSteg);
    }

    @Test
    void nesteSteg_forrigeStegErFørsteElement_forventAndreElement() {
        ProsessFlyt prosessFlyt = lagProsessFlyt();
        assertThat(prosessFlyt.nesteSteg(førsteSteg)).isEqualTo(andreSteg);
    }

    @Test
    void nesteSteg_forrigeStegErSisteElement_forventNull() {
        ProsessFlyt prosessFlyt = lagProsessFlyt();
        assertThat(prosessFlyt.nesteSteg(sisteSteg)).isNull();
    }

    @Test
    void nesteSteg_forrigeStegIkkeEnDelAvFlyt_kasterException() {
        ProsessFlyt prosessFlyt = lagProsessFlyt();
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> prosessFlyt.nesteSteg(ProsessSteg.AVSLUTT_TIDLIGERE_MEDL_PERIODE))
            .withMessageContaining("ikke gyldig for prosesstype");
    }

    @Test
    void opprettProsessflyt_medDuplikatSteg_kasterException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK, førsteSteg, andreSteg, sisteSteg, førsteSteg))
            .withMessageContaining("er definert to eller flere ganger");
    }

    private ProsessFlyt lagProsessFlyt() {
        return new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK, førsteSteg, andreSteg, sisteSteg);
    }

}