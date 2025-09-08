package no.nav.melosys.tjenester.gui.graphql.mapping

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StrukturertAdresseTilDtoKonverterTest {

    @Test
    fun `returnerer null for null strukturertAdresse`() {
        val result = StrukturertAdresseTilDtoKonverter.tilDto(null, mockk<KodeverkService>())
        assertNull(result)
    }

    @Test
    fun `returnerer dto for non-null strukturertAdresse`() {
        val strukturertAdresse = StrukturertAdresse("test", "test", "test", "test", "test", "test", "test", "NO")
        val kodeverkService = mockk<KodeverkService>()
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, strukturertAdresse.landkode) } returns "NO"

        val result = StrukturertAdresseTilDtoKonverter.tilDto(strukturertAdresse, kodeverkService)

        assertNotNull(result!!)
        assertEquals(strukturertAdresse.tilleggsnavn, result.tilleggsnavn)
        assertEquals(strukturertAdresse.gatenavn, result.gatenavn)
        assertEquals(strukturertAdresse.husnummerEtasjeLeilighet, result.husnummerEtasjeLeilighet)
        assertEquals("Postboks ${strukturertAdresse.postboks}", result.postboks)
        assertEquals(strukturertAdresse.postnummer, result.postnummer)
        assertEquals(strukturertAdresse.poststed, result.poststed)
        assertEquals(strukturertAdresse.region, result.region)
        assertEquals(strukturertAdresse.landkode, result.land)
    }

    @Test
    fun `mapper postboks korrekt når den ikke er satt`() {
        val strukturertAdresse = StrukturertAdresse("test", "test", "test", null, "test", "test", "test", "NO")
        val kodeverkService = mockk<KodeverkService>()
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, strukturertAdresse.landkode) } returns "NO"

        val result = StrukturertAdresseTilDtoKonverter.tilDto(strukturertAdresse, kodeverkService)

        assertNull(result!!.postboks)
    }
}
