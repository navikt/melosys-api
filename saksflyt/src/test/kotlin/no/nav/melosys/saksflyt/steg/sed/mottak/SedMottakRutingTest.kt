package no.nav.melosys.saksflyt.steg.sed.mottak

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter
import no.nav.melosys.service.eessi.ruting.SedRuterForSedTyper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class SedMottakRutingTest {

    private val sedRuterForSedTyper: SedRuterForSedTyper = mockk()
    private val defaultSedRuter: DefaultSedRuter = mockk()
    private val eessiService: EessiService = mockk()
    private val joarkFasade: JoarkFasade = mockk()

    private lateinit var sedMottakRuting: SedMottakRuting

    private val arkivsakID = 11L
    private val journalpost = Journalpost("123")

    @BeforeEach
    fun setUp() {
        journalpost.isErFerdigstilt = false
        sedMottakRuting = SedMottakRuting(setOf(sedRuterForSedTyper), defaultSedRuter, eessiService, joarkFasade)
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
    }

    @Test
    fun `utfør med sedType A009 skal kalle sedRuterForSedType`() {
        every { sedRuterForSedTyper.gjelderSedTyper() } returns setOf(SedType.A009)
        every { eessiService.finnSakForRinasaksnummer(any()) } returns Optional.of(arkivsakID)
        every { sedRuterForSedTyper.rutSedTilBehandling(any(), any()) } returns Unit

        val melosysEessiMelding = hentMelosysEessiMelding(SedType.A009)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }


        sedMottakRuting.utfør(prosessinstans)


        verify { sedRuterForSedTyper.rutSedTilBehandling(prosessinstans, arkivsakID) }
        verify(exactly = 0) { defaultSedRuter.rutSedTilBehandling(any(), any()) }
    }

    @Test
    fun `utfør med sedType X009 skal kalle manuell behandler`() {
        every { sedRuterForSedTyper.gjelderSedTyper() } returns setOf(SedType.A009)
        every { eessiService.finnSakForRinasaksnummer(any()) } returns Optional.of(arkivsakID)
        every { defaultSedRuter.rutSedTilBehandling(any(), any()) } returns Unit

        val melosysEessiMelding = hentMelosysEessiMelding(SedType.X009)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }


        sedMottakRuting.utfør(prosessinstans)


        verify(exactly = 0) { sedRuterForSedTyper.rutSedTilBehandling(any(), any()) }
        verify { defaultSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID) }
    }

    @Test
    fun `utfør med ferdigstilt journalpost skal ikke behandle videre`() {
        val melosysEessiMelding = hentMelosysEessiMelding(SedType.A009)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        journalpost.isErFerdigstilt = true


        sedMottakRuting.utfør(prosessinstans)


        verify(exactly = 0) { sedRuterForSedTyper.rutSedTilBehandling(any(), any<Long>()) }
        verify(exactly = 0) { defaultSedRuter.rutSedTilBehandling(any(), any<Long>()) }
    }

    private fun hentMelosysEessiMelding(sedType: SedType) = MelosysEessiMelding().apply {
        this.sedType = sedType.name
        rinaSaksnummer = "57483697"
        journalpostId = journalpost.journalpostId
    }
}
