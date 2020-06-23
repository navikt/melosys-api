package no.nav.melosys.integrasjon.eessi.dto;

import no.nav.melosys.domain.eessi.sed.Bestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class BestemmelseTest {

    @Test
    public void fraMelosysBestemmelse() {
        assertThat(Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1))
            .isEqualTo(Bestemmelse.ART_12_1);
        assertThat(Bestemmelse.fraMelosysBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1))
            .isEqualTo(Bestemmelse.ART_11_3_a);
        assertThat(Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
            .isEqualTo(Bestemmelse.ART_12_2);
        assertThat(Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4))
            .isEqualTo(Bestemmelse.ART_13_1_b_4);
        assertThat(Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2))
            .isEqualTo(Bestemmelse.ART_16_2);
    }

    @Test
    public void tilMelosysBestemmelse() {
        assertThat(Bestemmelse.ART_12_1.tilMelosysBestemmelse()).isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(Bestemmelse.ART_11_4.tilMelosysBestemmelse()).isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4);
    }

    @Test
    public void fraBestemmelseString() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Bestemmelse.fraBestemmelseString(null));
        assertThat(Bestemmelse.fraBestemmelseString("12_1")).isEqualTo(Bestemmelse.ART_12_1);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Bestemmelse.fraBestemmelseString(""));
    }
}
