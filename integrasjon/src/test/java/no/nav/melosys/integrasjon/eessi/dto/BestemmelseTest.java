package no.nav.melosys.integrasjon.eessi.dto;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BestemmelseTest {
    @Test
    public void map12_1FraMelosysBestemmelse_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
        Bestemmelse resultat = Bestemmelse.fraMelosysBestemmelse(lovvalgBestemmelse);

        assertThat(resultat).isEqualTo(Bestemmelse.ART_12_1);
    }

    @Test
    public void map11_4_1FraMelosysBestemmelse_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1;
        Bestemmelse resultat = Bestemmelse.fraMelosysBestemmelse(lovvalgBestemmelse);

        assertThat(resultat).isEqualTo(Bestemmelse.ART_11_3_a);
    }

    @Test
    public void map12_2FraMelosysBestemmelse_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2;
        Bestemmelse resultat = Bestemmelse.fraMelosysBestemmelse(lovvalgBestemmelse);

        assertThat(resultat).isEqualTo(Bestemmelse.ART_12_2);
    }

    @Test
    public void map13_1B4FraMelosysBestemmelse_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4;
        Bestemmelse resultat = Bestemmelse.fraMelosysBestemmelse(lovvalgBestemmelse);

        assertThat(resultat).isEqualTo(Bestemmelse.ART_13_1_b_4);
    }

    @Test
    public void map16_2FraMelosysBestemmelse_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2;
        Bestemmelse resultat = Bestemmelse.fraMelosysBestemmelse(lovvalgBestemmelse);

        assertThat(resultat).isEqualTo(Bestemmelse.ART_16_2);
    }

    @Test(expected = RuntimeException.class)
    public void mapNullFraMelosysBestemmelse_forventException() {
        Bestemmelse.fraMelosysBestemmelse(null);
    }

    @Test
    public void tilMelosysBestemmelse_12_1_forventKorrektBestemmelse() {
        Bestemmelse bestemmelse = Bestemmelse.ART_12_1;
        assertThat(bestemmelse.tilMelosysBestemmelse()).isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
    }

    @Test
    public void fraBestemmelseString_12_1_forventKorrektBestemmelse() {
        assertThat(Bestemmelse.fraBestemmelseString("12_1")).isEqualTo(Bestemmelse.ART_12_1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fraBestemmelseString_tomString_forventException() {
        Bestemmelse.fraBestemmelseString("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fraBestemmelseString_null_forventException() {
        Bestemmelse.fraBestemmelseString(null);
    }
}
