package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test

internal class LandkoderTest {

    @Test
    fun test() {
        val length = Landkoder.values().size
        length shouldBe EØS_MEDLEMER_OG_SVEITS + TERRITORIER
    }

    companion object {
        private const val EØS_MEDLEMER_OG_SVEITS = 32 // EU + EØS + EFTA
        private const val TERRITORIER = 4 // Grønland, Færøyene, Åland, Svalbard+JanMayen
    }
}