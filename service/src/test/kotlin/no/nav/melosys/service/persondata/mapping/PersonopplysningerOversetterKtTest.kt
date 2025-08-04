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
    lateinit var kodeverkService: KodeverkService

    @Test
    fun oversett() {
        every { kodeverkService.dekod(any(), any()) } returns "test"
        val personopplysninger = PersonopplysningerOversetter.oversett(lagPerson(), kodeverkService)

        personopplysninger.adressebeskyttelser shouldContainExactly listOf(
            Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, "PDL")
        )
        personopplysninger.bostedsadresse?.strukturertAdresse?.gatenavn shouldBe "gata"
        personopplysninger.dødsfall?.dødsdato shouldBe LocalDate.MAX
        personopplysninger.fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
        personopplysninger.folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
        personopplysninger.kjønn shouldBe KjoennType.UKJENT
        personopplysninger.navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
        personopplysninger.statsborgerskap shouldContainExactlyInAnyOrder listOf(
            Statsborgerskap("AIA", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, "PDL", "Dolly", false)
        )
    }
}
