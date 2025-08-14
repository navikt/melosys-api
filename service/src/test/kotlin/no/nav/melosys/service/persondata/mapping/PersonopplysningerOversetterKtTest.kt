package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse
import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class PersonopplysningerOversetterKtTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @Test
    fun `oversett skal mappe PDL person til personopplysninger`() {
        every { kodeverkService.dekod(any(), any()) } returns "test"


        val personopplysninger = PersonopplysningerOversetter.oversett(lagPerson(), kodeverkService)


        personopplysninger.run {
            adressebeskyttelser shouldContainExactly listOf(
                Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, "PDL")
            )
            bostedsadresse?.strukturertAdresse?.gatenavn shouldBe "gata"
            dødsfall?.dødsdato shouldBe LocalDate.MAX
            fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
            folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
            kjønn shouldBe KjoennType.UKJENT
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
            statsborgerskap shouldContainExactlyInAnyOrder listOf(
                Statsborgerskap("AIA", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
                Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, "PDL", "Dolly", false)
            )
        }
    }
}
