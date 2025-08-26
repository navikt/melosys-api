package no.nav.melosys.saksflyt.steg.sed

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OpprettSedGrunnlagTest {

    private lateinit var opprettSedGrunnlag: OpprettSedGrunnlag

    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService
    @MockK
    private lateinit var eessiService: EessiService

    @BeforeEach
    fun setup() {
        opprettSedGrunnlag = OpprettSedGrunnlag(mottatteOpplysningerService, eessiService)
    }

    @Test
    fun utfør() {
        val aktørID = "123"
        val melosysEessiMelding = MelosysEessiMelding().apply {
            rinaSaksnummer = "123"
            sedId = "abc"
        }
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 123321L
            }
            medData(ProsessDataKey.AKTØR_ID, aktørID)
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }

        every { eessiService.hentSedGrunnlag(any(), any()) } returns SedGrunnlag()
        every { mottatteOpplysningerService.opprettSedGrunnlag(any(), any()) } returns MottatteOpplysninger()


        opprettSedGrunnlag.utfør(prosessinstans)


        verify { mottatteOpplysningerService.opprettSedGrunnlag(prosessinstans.hentBehandling.id, any<SedGrunnlag>()) }
        verify { eessiService.hentSedGrunnlag("123", "abc") }
    }
}
