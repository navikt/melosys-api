package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.Personstatus
import no.nav.melosys.domain.dokument.person.Sivilstand
import no.nav.melosys.domain.person.*
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonMedHistorikkOversetterKtTest {
    private val kodeverkService: KodeverkService = mockk()

    @Test
    fun lagHistorikkFraTpsData() {
        val sivilstand = spyk<Sivilstand>()
        every { sivilstand.kode } returns "GLAD"
        every { kodeverkService.dekod(FellesKodeverk.PERSONSTATUSER, "ABNR") } returns "Aktivt BOSTNR"
        every { kodeverkService.dekod(FellesKodeverk.SIVILSTANDER, "GLAD") } returns "Gift, lever adskilt"

        val personDokumentFraTps = lagPersonDokument(sivilstand)
        val personMedHistorikk = PersonMedHistorikkOversetter.lagHistorikkFraTpsData(personDokumentFraTps, kodeverkService)

        personMedHistorikk.run {
            navn() shouldBe Navn("Kari", "Mellom", "Nordmann")
            kjønn() shouldBe KjoennType.KVINNE
            fødsel() shouldBe Foedsel(personDokumentFraTps.fødselsdato, 1989, null, null)
            folkeregisteridentifikator() shouldBe Folkeregisteridentifikator("123456789")
            bostedsadresser().shouldNotBeEmpty()
            kontaktadresser().shouldNotBeEmpty()
            oppholdsadresser().shouldBeEmpty()
            folkeregisterpersonstatuser().shouldNotBeEmpty()
            sivilstand().shouldNotBeEmpty()
            statsborgerskap().shouldNotBeEmpty()
            dødsfall() shouldBe Doedsfall(personDokumentFraTps.dødsdato)
        }
    }

    companion object {
        private fun lagPersonDokument(sivilstand: Sivilstand): PersonDokument {
            return PersonDokument().apply {
                kjønn = KjoennsType("K")
                fornavn = "Kari"
                mellomnavn = "Mellom"
                etternavn = "Nordmann"
                fødselsdato = LocalDate.parse("1989-08-07")
                fnr = "123456789"
                bostedsadresse = BrevDataTestUtils.lagBostedsadresse()
                postadresse = DokgenTestData.lagAdresse()
                personstatus = Personstatus.ABNR
                this.sivilstand = sivilstand
                sivilstandGyldighetsperiodeFom = LocalDate.parse("2019-08-07")
                statsborgerskap = Land(Land.NORGE)
                statsborgerskapDato = LocalDate.parse("1989-08-07")
                dødsdato = LocalDate.parse("2089-08-07")
            }
        }
    }
}
