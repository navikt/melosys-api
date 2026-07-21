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
    fun `utfør synker når SYNK_SAKSSTATUS_SAKSNUMMER-prosessdata er satt`() {
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SYNK_SAKSSTATUS_SAKSNUMMER, saksnummer)
        }
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) } just Runs

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 1) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer) }
    }

    @Test
    fun `utfør er no-op når nøkkelen mangler SELV OM behandling er satt`() {
        // Regresjonstest: SED-ruterne setter behandling på MOTTAK_SED-instansen under ruting —
        // en behandling-fallback ville synket for hver innkommende SED på en skjema-koblet sak
        val behandling = Behandling.forTest {
            fagsak { this.saksnummer = this@SynkSkjemaSaksstatusTest.saksnummer }
        }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 0) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(any()) }
    }

    @Test
    fun `utfør er no-op når nøkkelen mangler og generisk SAKSNUMMER-prosessdata er satt`() {
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSNUMMER, saksnummer)
        }

        synkSkjemaSaksstatus.utfør(prosessinstans)

        verify(exactly = 0) { skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(any()) }
    }
}
