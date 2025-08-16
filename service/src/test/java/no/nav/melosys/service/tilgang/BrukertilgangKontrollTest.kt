package no.nav.melosys.service.tilgang

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.sikkerhet.abac.Pep
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrukertilgangKontrollTest {

    @MockK
    private lateinit var pep: Pep

    private lateinit var brukertilgangKontroll: BrukertilgangKontroll

    @BeforeEach
    fun setup() {
        every { pep.sjekkTilgangTilAktoerId(any()) } returns Unit
        every { pep.sjekkTilgangTilFnr(any()) } returns Unit
        brukertilgangKontroll = BrukertilgangKontroll(pep)
    }

    @Test
    fun validerTilgangTilAktørID() {
        val aktørID = "11111"
        brukertilgangKontroll.validerTilgangTilAktørID(aktørID)
        verify { pep.sjekkTilgangTilAktoerId(aktørID) }
    }

    @Test
    fun validerTilgangTilFolkeregisterIdent() {
        val folkeregisterIdent = "123321"
        brukertilgangKontroll.validerTilgangTilFolkeregisterIdent(folkeregisterIdent)
        verify { pep.sjekkTilgangTilFnr(folkeregisterIdent) }
    }
}
