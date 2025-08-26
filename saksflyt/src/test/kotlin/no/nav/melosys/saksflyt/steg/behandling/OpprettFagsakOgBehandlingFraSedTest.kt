package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettFagsakOgBehandlingFraSedTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    private lateinit var opprettFagsakOgBehandlingFraSed: OpprettFagsakOgBehandlingFraSed

    private val opprettSakRequestSlot = slot<OpprettSakRequest>()

    @BeforeEach
    fun setUp() {
        opprettFagsakOgBehandlingFraSed = OpprettFagsakOgBehandlingFraSed(fagsakService, joarkFasade)
        every { fagsakService.nyFagsakOgBehandling(any()) } returns Fagsak.forTest {
            behandling {
                id = 1L
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
    }

    @Test
    fun `utfør skal verifisere at ny fagsak og behandling blir opprettet`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANMODNING_OM_UNNTAK
            medData(ProsessDataKey.SAKSTEMA, Sakstemaer.UNNTAK)
            medData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.BESLUTNING_LOVVALG_NORGE)
            medData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding().apply {
                rinaSaksnummer = "123rina"
                journalpostId = "123"
            })
        }
        every { joarkFasade.hentMottaksDatoForJournalpost(any()) } returns LocalDate.EPOCH


        opprettFagsakOgBehandlingFraSed.utfør(prosessinstans)


        verify { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) }
        opprettSakRequestSlot.captured.run {
            sakstype shouldBe Sakstyper.EU_EOS
            sakstema shouldBe Sakstemaer.UNNTAK
            behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
            behandlingsårsaktype shouldBe Behandlingsaarsaktyper.SED
            mottaksdato.shouldNotBeNull()
        }
    }
}
