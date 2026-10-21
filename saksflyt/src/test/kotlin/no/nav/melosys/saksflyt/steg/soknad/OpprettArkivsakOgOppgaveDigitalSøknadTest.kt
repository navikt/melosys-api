package no.nav.melosys.saksflyt.steg.soknad

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.ArkivsakService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpprettArkivsakOgOppgaveDigitalSøknadTest {

    private val fagsakService: FagsakService = mockk(relaxed = true)
    private val arkivsakService: ArkivsakService = mockk()
    private val oppgaveService: OppgaveService = mockk(relaxed = true)

    private val opprettArkivsak = OpprettArkivsakDigitalSøknad(fagsakService, arkivsakService, OppgaveFactory())
    private val opprettOppgave = OpprettOppgaveDigitalSøknad(oppgaveService)

    private fun prosessinstans(nySak: Boolean) = Prosessinstans.forTest {
        type = ProsessType.MOTTAK_DIGITAL_SØKNAD
        behandling { fagsak { medBruker() } }
        medData(ProsessDataKey.DIGITAL_SØKNAD_NY_SAK, nySak)
    }

    @Test
    fun `oppretter arkivsak når søknaden ga ny sak`() {
        every { arkivsakService.opprettSakForBruker(any(), any(), any()) } returns 123L

        opprettArkivsak.utfør(prosessinstans(nySak = true))

        verify { arkivsakService.opprettSakForBruker(any(), any(), any()) }
    }

    @Test
    fun `oppretter ikke arkivsak når søknaden gjelder eksisterende sak`() {
        opprettArkivsak.utfør(prosessinstans(nySak = false))

        verify(exactly = 0) { arkivsakService.opprettSakForBruker(any(), any(), any()) }
        verify(exactly = 0) { fagsakService.lagre(any()) }
    }

    @Test
    fun `oppretter oppgave når søknaden ga ny sak`() {
        opprettOppgave.utfør(prosessinstans(nySak = true))

        verify { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any(), true) }
    }

    @Test
    fun `oppretter ikke oppgave når søknaden gjelder eksisterende sak`() {
        opprettOppgave.utfør(prosessinstans(nySak = false))

        verify(exactly = 0) { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any(), any()) }
    }
}
