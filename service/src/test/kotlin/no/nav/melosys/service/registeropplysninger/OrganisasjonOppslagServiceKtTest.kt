package no.nav.melosys.service.registeropplysninger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OrganisasjonOppslagServiceKtTest {

    @MockK
    private lateinit var eregFasade: EregFasade

    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @BeforeEach
    fun setup() {
        organisasjonOppslagService = OrganisasjonOppslagService(eregFasade)
    }

    @Test
    fun `hentOrganisasjon gyldigOrgnrMedTommeMellomrom returnererOrganisasjon`() {
        val orgnrMedWhitespace = " 123456789 "

        val saksopplysning = Saksopplysning()
        saksopplysning.dokument = OrganisasjonDokumentTestFactory.builder().build()
        every { eregFasade.hentOrganisasjon(orgnrMedWhitespace.trim()) } returns saksopplysning

        val result = organisasjonOppslagService.hentOrganisasjon(orgnrMedWhitespace)
        result.shouldBeInstanceOf<OrganisasjonDokument>()
    }

    @Test
    fun `hentOrganisasjon ugyldigOrgnr kasterFeil`() {
        val ugyldigOrgnr = "1"

        val exception = shouldThrow<FunksjonellException> {
            organisasjonOppslagService.hentOrganisasjon(ugyldigOrgnr)
        }
        exception.message shouldContain "Ugyldig orgnr"
    }
}
