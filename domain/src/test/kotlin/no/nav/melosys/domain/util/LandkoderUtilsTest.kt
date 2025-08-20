package no.nav.melosys.domain.util

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

internal class LandkoderUtilsTest {

    @Test
    fun valiateLandkoderTest() {
        IsoLandkodeKonverterer.tilIso3(Landkoder.NO.kode) shouldBe Land.NORGE
    }

    @Test
    fun validerSammenhengMellomTypeOgKonvertering() {
        // Sjekker at alle iso2-koder er inkludert begge mappere
        for (landkodeIso2 in Landkoder.values()) {
            val landkodeIso3 = IsoLandkodeKonverterer.tilIso3(landkodeIso2.kode)
            val resultatSomIso2 = IsoLandkodeKonverterer.tilIso2(landkodeIso3)


            landkodeIso2.kode shouldBe resultatSomIso2
        }

        // Sjekker at alle iso3-koder er inkludert i mappere (Bortsett fra Statsløs og Ukjent)
        for (landkodeIso3 in hentLandIso3()) {
            if (landkodeIso3 == Land.STATSLØS) continue
            if (landkodeIso3 == Land.UKJENT) continue

            val landkodeIso2 = IsoLandkodeKonverterer.tilIso2(landkodeIso3)
            val resultatSomIso3 = IsoLandkodeKonverterer.tilIso3(landkodeIso2)


            landkodeIso3 shouldBe resultatSomIso3
        }
    }

    private fun hentLandIso3(): List<String> {
        val landkoderIso3 = mutableListOf<String>()
        val fields = Land::class.java.declaredFields
        for (felt in fields) {
            if (Modifier.isPublic(felt.modifiers) &&
                Modifier.isStatic(felt.modifiers) &&
                felt.get(null) is String
            ) {
                landkoderIso3.add(felt.get(null) as String)
            }
        }
        return landkoderIso3
    }
}
