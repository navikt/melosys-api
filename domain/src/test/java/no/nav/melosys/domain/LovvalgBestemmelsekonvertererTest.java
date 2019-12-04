package no.nav.melosys.domain;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class LovvalgBestemmelsekonvertererTest {

    private final LovvalgBestemmelsekonverterer instans;
    
    public LovvalgBestemmelsekonvertererTest() {
        instans = new LovvalgBestemmelsekonverterer();
    }

    @Test
    public void konverterFra883_2004TilDbKolonneGirStreng() {
        testConvertToDatabseColumn(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, "ART16_2");
    }
    
    @Test
    public void konverterFra987_2009TilDbKolonneGirStreng() {
        testConvertToDatabseColumn(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11, "ART14_11");
    }

    @Test
    public void konverterFraTillegg883_2004TilDbKolonneGirStreng() {
        testConvertToDatabseColumn(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2, "ART11_2");
    }

    @Test
    public void konverterFraNullTilDbKolonneGirNull() {
        testConvertToDatabseColumn(null, null);
    }

    private void testConvertToDatabseColumn(LovvalgBestemmelse input, String forventet) {
        String resultat = instans.convertToDatabaseColumn(input);
        if (input == null) {
            assertThat(resultat).isNull();
        } else {
            assertThat(resultat).endsWith(forventet);
        }
    }

    @Test
    public void konverter883_2004TilEntitsattributtGirOppramsInstans() {
        testKonverterTIlEntitetsAttributt(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
    }

    @Test
    public void konverter987_2009TilEntitsattributtGirOppramsInstans() {
        testKonverterTIlEntitetsAttributt(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11);
    }

    @Test
    public void konverterTillegg883_2004TilEntitsattributtGirOppramsInstans() {
        testKonverterTIlEntitetsAttributt(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A);
    }

    @Test
    public void konverterNullTilEntitsattributtGirNull() {
        testKonverterTIlEntitetsAttributt(null);
    }

    private void testKonverterTIlEntitetsAttributt(LovvalgBestemmelse input) {
        LovvalgBestemmelse resultat = instans.convertToEntityAttribute(input != null ? input.name() : null);
        assertThat(resultat).isEqualTo(input);
    }

    @Test
    public void konverterUkjentOppramsverdiKasterUnntak() throws Throwable {
        Throwable thrown = catchThrowable(() -> instans.convertToEntityAttribute("Brottskavl"));
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No enum constant");
    }
}

