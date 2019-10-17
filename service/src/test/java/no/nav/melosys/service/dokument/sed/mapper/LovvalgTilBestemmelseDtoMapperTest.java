package no.nav.melosys.service.dokument.sed.mapper;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.integrasjon.eessi.dto.Bestemmelse;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LovvalgTilBestemmelseDtoMapperTest {

    @Test
    public void map12_1TilBestemmelseDto_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
        Bestemmelse resultat = LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgBestemmelse);

        assertThat(resultat, is(Bestemmelse.ART_12_1));
    }

    @Test
    public void map12_2TilBestemmelseDto_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2;
        Bestemmelse resultat = LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgBestemmelse);

        assertThat(resultat, is(Bestemmelse.ART_12_2));
    }

    @Test
    public void map11_4_1TilBestemmelseDto_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1;
        Bestemmelse resultat = LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgBestemmelse);

        assertThat(resultat, is(Bestemmelse.ART_11_3_a));
    }

    @Test
    public void map16_2TilBestemmelseDto_forventKorrektBestemmelse() {
        LovvalgBestemmelse lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2;
        Bestemmelse resultat = LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgBestemmelse);

        assertThat(resultat, is(Bestemmelse.ART_16_2));
    }

    @Test(expected = RuntimeException.class)
    public void mapNullTilBestemmelseDto_forventException() {
        LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(null);
    }

}