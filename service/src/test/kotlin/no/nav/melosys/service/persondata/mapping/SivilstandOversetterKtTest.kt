package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.person.Sivilstand
import no.nav.melosys.domain.person.Sivilstandstype
import no.nav.melosys.service.persondata.PdlObjectFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SivilstandOversetterKtTest {

    @Test
    fun `oversettForRelatertVedSivilstand skal oversette PDL sivilstand til domeneobjekt`() {
        val pdlSivilstandListe = PdlObjectFactory.lagPerson().sivilstand()


        val sivilstand = SivilstandOversetter.oversettForRelatertVedSivilstand(pdlSivilstandListe)


        sivilstand shouldBe Sivilstand(
            Sivilstandstype.GIFT, null, "relatertVedSivilstandID",
            LocalDate.MIN, LocalDate.EPOCH, "PDL", "Dolly", false
        )
    }
}
