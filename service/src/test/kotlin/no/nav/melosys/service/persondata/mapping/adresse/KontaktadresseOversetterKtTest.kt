package no.nav.melosys.service.persondata.mapping.adresse

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.*
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.metadata
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class KontaktadresseOversetterKtTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @Test
    fun `oversettVegadresse skal oversette norsk vegadresse korrekt`() {
        val kontaktadressePDL = Kontaktadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            null,
            null,
            Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            metadata()
        )
        every { kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234")) } returns "Bergen"


        val kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService)


        kontaktadresse.run {
            coAdressenavn() shouldBe "Kari Hansen"
            gyldigFraOgMed() shouldBe LocalDate.parse("2020-01-01")
            gyldigTilOgMed() shouldBe LocalDate.parse("2020-05-05")
            strukturertAdresse().run {
                gatenavn shouldBe "Kirkegata"
                husnummerEtasjeLeilighet shouldBe "12 B"
                postnummer shouldBe "1234"
                poststed shouldBe "Bergen"
                region.shouldBeNull()
                landkode shouldBe "NO"
            }
            registrertDato() shouldBe kontaktadressePDL.metadata().datoSistRegistrert()
            master() shouldBe "PDL"
            kilde() shouldBe "Dolly"
        }
    }

    @Test
    fun `oversettPostadresseIFrittFormat skal oversette postadresse i fritt format`() {
        val kontaktadressePDL = Kontaktadresse(
            null,
            null,
            null,
            null,
            PostadresseIFrittFormat("1", "2", "3", "1234"),
            null,
            null,
            null,
            metadata()
        )
        every { kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234")) } returns "Enby"


        val kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService)


        kontaktadresse.semistrukturertAdresse().run {
            adresselinje1 shouldBe "1"
            adresselinje2 shouldBe "2"
            adresselinje3 shouldBe "3"
            adresselinje4.shouldBeNull()
            postnr shouldBe "1234"
            poststed shouldBe "Enby"
            landkode shouldBe "NO"
        }
    }

    @Test
    fun `oversettUtenlandskAdresse skal oversette utenlandsk adresse korrekt`() {
        val kontaktadressePDL = Kontaktadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            null,
            UtenlandskAdresse(
                "adressenavnNummer",
                "bygningEtasjeLeilighet",
                "P.O.Box 1234 Place",
                "SE-12345",
                "Haworth",
                "Yorkshire",
                "SWE"
            ),
            null,
            null,
            metadata()
        )


        val kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService)


        kontaktadresse.strukturertAdresse().run {
            gatenavn shouldBe "adressenavnNummer"
            husnummerEtasjeLeilighet shouldBe "bygningEtasjeLeilighet"
            postboks shouldBe "P.O.Box 1234 Place"
            postnummer shouldBe "SE-12345"
            poststed shouldBe "Haworth"
            region shouldBe "Yorkshire"
            landkode shouldBe "SE"
        }
    }

    @Test
    fun `oversettUtenlandskAdresseIFrittFormat skal oversette utenlandsk adresse i fritt format`() {
        val kontaktadressePDL = Kontaktadresse(
            null,
            null,
            null,
            null,
            null,
            null,
            UtenlandskAdresseIFrittFormat(
                "1",
                "2",
                "3",
                "postkode",
                "by",
                "FRA"
            ),
            null,
            metadata()
        )


        val kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService)


        kontaktadresse.semistrukturertAdresse().run {
            adresselinje1 shouldBe "1"
            adresselinje2 shouldBe "2"
            adresselinje3 shouldBe "3"
            adresselinje4.shouldBeNull()
            postnr shouldBe "postkode"
            poststed shouldBe "by"
            landkode shouldBe "FR"
        }
    }

    @Test
    fun `oversettPostboksAdresse skal oversette postboksadresse korrekt`() {
        val kontaktadressePDL = Kontaktadresse(
            null,
            null,
            null,
            Postboksadresse(
                "Byggfirma A/S",
                "Postboks 1234",
                "1234"
            ),
            null,
            null,
            null,
            null,
            metadata()
        )
        every { kodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), eq("1234")) } returns "Bergen"


        val kontaktadresse = KontaktadresseOversetter.oversett(kontaktadressePDL, kodeverkService)


        kontaktadresse.run {
            coAdressenavn() shouldBe "Byggfirma A/S"
            strukturertAdresse().run {
                postnummer shouldBe "1234"
                postboks shouldBe "Postboks 1234"
            }
        }
    }
}
