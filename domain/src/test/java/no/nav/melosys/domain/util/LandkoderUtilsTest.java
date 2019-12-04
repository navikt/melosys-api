package no.nav.melosys.domain.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LandkoderUtilsTest {

    @Test
    public void valiateLandkoderTest() {
        assertThat(LandkoderUtils.tilIso3(Landkoder.NO.getKode())).isEqualTo(Land.NORGE);
    }

    @Test
    public void validerSammenhengMellomTypeOgKonvertering() throws IllegalAccessException {
        // Sjekker at alle iso2-koder er inkludert begge mappere
        for (Landkoder landkodeIso2 : Landkoder.values()) {
            String landkodeIso3 = LandkoderUtils.tilIso3(landkodeIso2.getKode());
            String resultatSomIso2 = LandkoderUtils.tilIso2(landkodeIso3);
            assertThat(landkodeIso2.getKode()).isEqualTo(resultatSomIso2);
        }

        // Sjekker at alle iso3-koder er inkludert i mappere (Bortsett fra Statsløs og Ukjent)
        for (String landkodeIso3 : hentLandIso3()) {
            if (Objects.equals(landkodeIso3, Land.STATSLØS)) continue;
            if (Objects.equals(landkodeIso3, Land.UKJENT)) continue;

            String landkodeIso2 = LandkoderUtils.tilIso2(landkodeIso3);
            String resultatSomIso3 = LandkoderUtils.tilIso3(landkodeIso2);
            assertThat(landkodeIso3).isEqualTo(resultatSomIso3);
        }
    }

    private List<String> hentLandIso3() throws IllegalAccessException {
        List<String> landkoderIso3 = new ArrayList<>();
        Field[] fields = Land.class.getDeclaredFields();
        for (Field felt : fields) {
            if (Modifier.isPublic(felt.getModifiers()) &&
                Modifier.isStatic(felt.getModifiers()) &&
                felt.get(null) instanceof String) {
                landkoderIso3.add((String) felt.get(null));
            }
        }
        return landkoderIso3;
    }
}