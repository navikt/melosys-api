package no.nav.melosys.service.persondata.mapping.adresse

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.List
import kotlin.jvm.optionals.getOrNull

@ExtendWith(MockKExtension::class)
class BostedsadresseOversetterKtTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @Test
    fun `finnOgOversett skal finne og oversette gyldig bostedsadresse`() {
        val ugyldigBostedsadresse = lagUgyldigBostedsadresse()
        val gyldigBostedsadresse = lagNorskBostedsadresse()
        val addresser = List.of(ugyldigBostedsadresse, gyldigBostedsadresse)
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234") } returns "Bergen"


        val result = BostedsadresseOversetter.finnGjeldende(addresser, kodeverkService)


        result.strukturertAdresse().gatenavn shouldBe gyldigBostedsadresse.vegadresse().adressenavn()
    }

    @Test
    fun `oversettVegadresse skal oversette norsk vegadresse korrekt`() {
        val bostedsadressePDL = lagNorskBostedsadresse()
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234") } returns "Bergen"


        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService)


        bostedsadresseOptional.getOrNull().shouldNotBeNull().run {
            coAdressenavn() shouldBe "Kari Hansen"
            gyldigFraOgMed() shouldBe LocalDate.parse("2020-01-01")
            gyldigTilOgMed() shouldBe LocalDate.parse("2020-05-05")
            strukturertAdresse().run {
                gatenavn shouldBe "Kirkegata"
                husnummerEtasjeLeilighet shouldBe "12 B"
                tilleggsnavn shouldBe "Storgården"
                postnummer shouldBe "1234"
                poststed shouldBe "Bergen"
                region.shouldBeNull()
                landkode shouldBe "NO"
            }
            master() shouldBe "PDL"
            kilde() shouldBe "Dolly"
        }
    }

    @Test
    fun `oversettMatrikkeladresse skal oversette matrikkeladresse korrekt`() {
        val bostedsadresseMedMatrikkelAdresse = lagBostedsadresseMedMatrikkelAdresse()
        every { kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), any()) } returns "Asker"


        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadresseMedMatrikkelAdresse, kodeverkService)


        bostedsadresseOptional.getOrNull().shouldNotBeNull().strukturertAdresse().run {
            gatenavn.shouldBeNull()
            husnummerEtasjeLeilighet.shouldBeNull()
            tilleggsnavn shouldBe "tilleggsnavn"
            postnummer shouldBe "4321"
            poststed shouldBe "Asker"
            region.shouldBeNull()
            landkode shouldBe "NO"
        }
    }

    @Test
    fun `oversettUtenlandskAdresse skal oversette utenlandsk adresse korrekt`() {
        val bostedsadressePDL = lagUtenlandskBostedsadresse()


        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService)


        bostedsadresseOptional.getOrNull().shouldNotBeNull().strukturertAdresse().run {
            gatenavn shouldBe "adressenavnNummer"
            husnummerEtasjeLeilighet shouldBe "bygningEtasjeLeilighet"
            postboks shouldBe "P.O.Box 1234 Place"
            postnummer shouldBe "SE-12345"
            poststed shouldBe "Haworth"
            region shouldBe "Yorkshire"
            landkode shouldBe "SE"
        }
    }
}
