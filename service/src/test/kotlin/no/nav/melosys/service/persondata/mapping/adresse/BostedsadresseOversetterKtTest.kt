package no.nav.melosys.service.persondata.mapping.adresse

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.List

class BostedsadresseOversetterKtTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun finnOgOversett() {
        val ugyldigBostedsadresse = lagUgyldigBostedsadresse()
        val gyldigBostedsadresse = lagNorskBostedsadresse()
        val addresser = List.of(ugyldigBostedsadresse, gyldigBostedsadresse)
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234") } returns "Bergen"

        val result = BostedsadresseOversetter.finnGjeldende(addresser, kodeverkService)

        result.strukturertAdresse().gatenavn shouldBe gyldigBostedsadresse.vegadresse().adressenavn()
    }

    @Test
    fun oversettVegadresse() {
        val bostedsadressePDL = lagNorskBostedsadresse()
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234") } returns "Bergen"

        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService)

        bostedsadresseOptional.shouldNotBeNull()
        bostedsadresseOptional.get().run {
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
    fun oversettMatrikkeladresse() {
        val bostedsadresseMedMatrikkelAdresse = lagBostedsadresseMedMatrikkelAdresse()
        every { kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), any()) } returns "Asker"

        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadresseMedMatrikkelAdresse, kodeverkService)

        bostedsadresseOptional.shouldNotBeNull()
        bostedsadresseOptional.get().strukturertAdresse().run {
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
    fun oversettUtenlandskAdresse() {
        val bostedsadressePDL = lagUtenlandskBostedsadresse()

        val bostedsadresseOptional = BostedsadresseOversetter.oversett(bostedsadressePDL, kodeverkService)

        bostedsadresseOptional.shouldNotBeNull()
        bostedsadresseOptional.get().strukturertAdresse().run {
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
