package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AvklartMaritimtArbeidTest {

    public static Avklartefakta lagAvklartefaktaSokkelSkip(String navn, String maritimType) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatyper.SOKKEL_ELLER_SKIP);
        avklartefakta.setSubjekt(navn);
        avklartefakta.setFakta(maritimType);
        return avklartefakta;
    }

    public static Avklartefakta lagAvklartefaktaArbeidsland(String navn, String landkode) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        avklartefakta.setSubjekt(navn);
        avklartefakta.setFakta(landkode);
        return avklartefakta;
    }

    @Test
    public void leggTilFakta_medTypeSokkel_girMaritimTypeSokkel() {
        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("Stena Don");
        avklartMaritimtArbeid.leggTilFakta(lagAvklartefaktaSokkelSkip("Stena Don", Maritimtyper.SOKKEL.getKode()));

        assertThat(avklartMaritimtArbeid.getMaritimtype()).isEqualTo(Maritimtyper.SOKKEL);
        assertThat(avklartMaritimtArbeid.getLand()).isNull();
        assertThat(avklartMaritimtArbeid.getNavn()).isEqualTo("Stena Don");
    }

    @Test
    public void leggTilFakta_medTypeArbeidsland_girArbeidsland() {
        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("Stena Don");
        avklartMaritimtArbeid.leggTilFakta(lagAvklartefaktaArbeidsland("Stena Don", Landkoder.GB.getKode()));

        assertThat(avklartMaritimtArbeid.getLand()).isEqualTo(Landkoder.GB.getKode());
        assertThat(avklartMaritimtArbeid.getMaritimtype()).isNull();
        assertThat(avklartMaritimtArbeid.getNavn()).isEqualTo("Stena Don");
    }
}
