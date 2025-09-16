package no.nav.melosys.service.persondata.mapping.adresse

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Oppholdsadresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.metadata
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class OppholdsadresseOversetterTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @Test
    fun `oversettVegadresse skal oversette vegadresse korrekt`() {
        val oppholdsadressePDL = Oppholdsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            "Kari Hansen",
            null,
            Vegadresse(
                "Kirkegata",
                "12",
                "B",
                "Storgården",
                "1234"
            ),
            null,
            metadata()
        )
        every { kodeverkService.dekod(FellesKodeverk.POSTNUMMER, "1234") } returns "Bergen"


        val oppholdsadresse = OppholdsadresseOversetter.oversett(oppholdsadressePDL, kodeverkService)


        oppholdsadresse.shouldNotBeNull().run {
            coAdressenavn shouldBe "Kari Hansen"
            gyldigFraOgMed shouldBe LocalDate.parse("2020-01-01")
            gyldigTilOgMed shouldBe LocalDate.parse("2020-05-05")
            strukturertAdresse?.run {
                gatenavn shouldBe "Kirkegata"
                husnummerEtasjeLeilighet shouldBe "12 B"
                tilleggsnavn shouldBe "Storgården"
                postnummer shouldBe "1234"
                poststed shouldBe "Bergen"
                region shouldBe null
                landkode shouldBe "NO"
            }
            registrertDato shouldBe oppholdsadressePDL.metadata().datoSistRegistrert()
            master shouldBe "PDL"
            kilde shouldBe "Dolly"
        }
    }

    @Test
    fun `oversettUtenlandskAdresse skal oversette utenlandsk adresse korrekt`() {
        val oppholdsadressePDL = Oppholdsadresse(
            null,
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


        val oppholdsadresse = OppholdsadresseOversetter.oversett(oppholdsadressePDL, kodeverkService)


        oppholdsadresse.shouldNotBeNull().strukturertAdresse?.run {
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
    fun `oversettTomOppholdsadresse skal returnere null for tom oppholdsadresse`() {
        val oppholdsadressePDL = Oppholdsadresse(
            null,
            null,
            null,
            null,
            null,
            null,
            metadata()
        )


        val oppholdsadresse = OppholdsadresseOversetter.oversett(oppholdsadressePDL, kodeverkService)


        oppholdsadresse shouldBe null
    }
}
