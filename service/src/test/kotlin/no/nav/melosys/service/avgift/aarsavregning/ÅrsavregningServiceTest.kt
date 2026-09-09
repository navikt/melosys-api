package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val SAKSNUMMER = "MEL-8161"

@ExtendWith(MockKExtension::class)
class ÅrsavregningServiceTest {

    @MockK(relaxed = true)
    private lateinit var aarsavregningRepository: AarsavregningRepository

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK(relaxed = true)
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    private val service: ÅrsavregningService by lazy {
        ÅrsavregningService(aarsavregningRepository, behandlingsresultatService, fagsakService, trygdeavgiftService)
    }

    @Test
    fun `harAktivÅrsavregningForÅr - åpen årsavregning uten år (mangler aarsavregning-rad) blokkerer ikke`() {
        val behandling = aarsavregningBehandling(id = 100L)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns behandling.fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(100L) } returns Behandlingsresultat.forTest { }

        service.harAktivÅrsavregningForÅr(SAKSNUMMER, 2024) shouldBe false
    }

    @Test
    fun `harAktivÅrsavregningForÅr - aktiv årsavregning med samme år blokkerer (og er år-spesifikk)`() {
        val behandling = aarsavregningBehandling(id = 100L)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns behandling.fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(100L) } returns
            Behandlingsresultat.forTest { årsavregning { aar = 2024 } }

        service.harAktivÅrsavregningForÅr(SAKSNUMMER, 2024) shouldBe true
        service.harAktivÅrsavregningForÅr(SAKSNUMMER, 2025) shouldBe false
    }

    @Test
    fun `harAktivÅrsavregningForÅr - ekte feil (ikke manglende aarsavregning-rad) propageres, svelges ikke`() {
        val behandling = aarsavregningBehandling(id = 100L)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns behandling.fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(100L) } throws RuntimeException("simulert DB-feil")

        shouldThrow<RuntimeException> {
            service.harAktivÅrsavregningForÅr(SAKSNUMMER, 2024)
        }
    }

    @Test
    fun `harAktivÅrsavregningForÅr - ingen aktiv årsavregningsbehandling gir false`() {
        val fagsak = Fagsak.forTest {
            saksnummer = SAKSNUMMER
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
            }
        }
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak

        service.harAktivÅrsavregningForÅr(SAKSNUMMER, 2024) shouldBe false
    }

    private fun aarsavregningBehandling(id: Long): Behandling = Behandling.forTest {
        this.id = id
        type = Behandlingstyper.ÅRSAVREGNING
        status = Behandlingsstatus.UNDER_BEHANDLING
        fagsak { saksnummer = SAKSNUMMER }
    }
}
