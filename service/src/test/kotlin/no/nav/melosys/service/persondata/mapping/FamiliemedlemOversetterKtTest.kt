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
    fun oversettBarn() {
        val familiemedlem = FamiliemedlemOversetter.oversettBarn(lagPerson(), Folkeregisteridentifikator("identForelder1"))

        familiemedlem.folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
        familiemedlem.fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
        familiemedlem.familierelasjon shouldBe Familierelasjon.BARN
        familiemedlem.navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
        familiemedlem.foreldreansvarstype shouldBe "felles"
        familiemedlem.folkeregisteridentAnnenForelder shouldBe Folkeregisteridentifikator("forelderIdent")
    }

    @Test
    fun oversettForelder() {
        val familiemedlem = FamiliemedlemOversetter.oversettForelder(lagPerson(), Familierelasjonsrolle.MOR)

        familiemedlem.folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
        familiemedlem.familierelasjon shouldBe Familierelasjon.MOR
        familiemedlem.navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
    }

    @Test
    fun oversettPersonRelatertVedSivilstandMedSivilstand() {
        val forventetSivilstandID = "forventetSivilstandID"

        val familiemedlem = FamiliemedlemOversetter.oversettEktefelleEllerPartner(lagPerson(), lagSivilstand(forventetSivilstandID))

        familiemedlem.folkeregisteridentifikator shouldBe Folkeregisteridentifikator("IdNr")
        familiemedlem.fødsel shouldBe Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested")
        familiemedlem.familierelasjon shouldBe RELATERT_VED_SIVILSTAND
        familiemedlem.navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
        familiemedlem.sivilstand.shouldNotBeNull()
        familiemedlem.sivilstand()!!.relatertVedSivilstand shouldBe forventetSivilstandID
    }
}
