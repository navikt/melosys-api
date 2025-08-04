package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.person.Sivilstand
import no.nav.melosys.domain.person.Sivilstandstype
import no.nav.melosys.service.persondata.PdlObjectFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SivilstandOversetterKtTest {

    @Test
    fun `oversettForRelatertVedSivilstand`() {
        val sivilstand = SivilstandOversetter.oversettForRelatertVedSivilstand(PdlObjectFactory.lagPerson().sivilstand())
        sivilstand shouldBe Sivilstand(
            Sivilstandstype.GIFT, null, "relatertVedSivilstandID",
            LocalDate.MIN, LocalDate.EPOCH, "PDL", "Dolly", false
        )
    }
}
