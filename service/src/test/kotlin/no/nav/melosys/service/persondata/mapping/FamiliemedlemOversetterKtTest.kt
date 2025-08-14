package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.person.Foedsel
import no.nav.melosys.domain.person.Folkeregisteridentifikator
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.familie.Familierelasjon
import no.nav.melosys.domain.person.familie.Familierelasjon.RELATERT_VED_SIVILSTAND
import no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle
import no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson
import no.nav.melosys.service.persondata.PdlObjectFactory.lagSivilstand
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FamiliemedlemOversetterKtTest {

    @Test
    fun `oversettBarn skal oversette barn med alle felter korrekt`() {
        val familiemedlem = FamiliemedlemOversetter.oversettBarn(lagPerson(), Folkeregisteridentifikator("identForelder1"))


        familiemedlem.run {
            folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
            fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
            familierelasjon shouldBe Familierelasjon.BARN
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
            foreldreansvarstype shouldBe "felles"
            folkeregisteridentAnnenForelder shouldBe Folkeregisteridentifikator("forelderIdent")
        }
    }

    @Test
    fun `oversettForelder skal oversette forelder med korrekt familierelasjon`() {
        val familiemedlem = FamiliemedlemOversetter.oversettForelder(lagPerson(), Familierelasjonsrolle.MOR)


        familiemedlem.run {
            folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
            familierelasjon shouldBe Familierelasjon.MOR
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
        }
    }

    @Test
    fun `oversettEktefelleEllerPartner skal oversette person relatert ved sivilstand med sivilstand`() {
        val forventetSivilstandID = "forventetSivilstandID"


        val familiemedlem = FamiliemedlemOversetter.oversettEktefelleEllerPartner(lagPerson(), lagSivilstand(forventetSivilstandID))


        familiemedlem.run {
            folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
            fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
            familierelasjon shouldBe RELATERT_VED_SIVILSTAND
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
            sivilstand.shouldNotBeNull()
            sivilstand()!!.relatertVedSivilstand shouldBe forventetSivilstandID
        }
    }
}
