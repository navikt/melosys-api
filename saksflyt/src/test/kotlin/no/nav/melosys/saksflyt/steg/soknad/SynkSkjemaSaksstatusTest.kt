package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.sak.SkjemaSaksstatusSyncService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SynkSkjemaSaksstatusTest {

    @MockK
    lateinit var skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService

    private lateinit var synkSkjemaSaksstatus: SynkSkjemaSaksstatus

    private val saksnummer = "MEL-1234"

    @BeforeEach
    fun setup() {
        synkSkjemaSaksstatus = SynkSkjemaSaksstatus(skjemaSaksstatusSyncService)
    }

    @Test
    fun `inngangsSteg returnerer SYNK_SKJEMA_SAKSSTATUS`() {
        synkSkjemaSaksstatus.inngangsSteg() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
    }

    @Test
    fun `utfør bruker saksnummer fra prosessdata når satt`() {
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSNUMMER, saksnummer)
        }
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) } just Runs

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 1) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) }
    }

    @Test
    fun `utfør faller tilbake til behandlingens fagsak når SAKSNUMMER-prosessdata mangler`() {
        val behandling = Behandling.forTest {
            fagsak { this.saksnummer = this@SynkSkjemaSaksstatusTest.saksnummer }
        }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) } just Runs

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 1) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) }
    }

    @Test
    fun `utfør er no-op når verken SAKSNUMMER-prosessdata eller behandling er satt`() {
        val prosessinstans = Prosessinstans.forTest { }

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 0) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(any()) }
    }
}
