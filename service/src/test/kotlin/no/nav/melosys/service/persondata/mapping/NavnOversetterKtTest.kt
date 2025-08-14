package no.nav.melosys.service.persondata.mapping

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.integrasjon.pdl.dto.Endring
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.KORRIGER
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT
import no.nav.melosys.integrasjon.pdl.dto.Metadata
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import no.nav.melosys.integrasjon.pdl.dto.person.Navn as PdlNavn

class NavnOversetterKtTest {

    @Test
    fun `oversettTilModel skal hente nyeste ikke-historisk navn når det er flere navn`() {
        val navn = setOf(
            PdlNavn("fornavn", "histo", "risk", historiskMetadata()),
            PdlNavn("fornavn", "mellomnavn", "nyetternavn", metadata())
        )


        val resultat = NavnOversetter.oversett(navn)


        resultat shouldBe Navn("fornavn", "mellomnavn", "nyetternavn")
    }

    private fun metadata(): Metadata = Metadata(
        "PDL", false,
        listOf(Endring(OPPRETT, LocalDateTime.parse("2022-03-16T10:04:52"), "Dolly"))
    )

    private fun historiskMetadata(): Metadata = Metadata(
        "PDL", true,
        listOf(
            Endring(OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly"),
            Endring(KORRIGER, LocalDateTime.parse("2022-03-16T12:04:52"), "Dolly")
        )
    )
}
