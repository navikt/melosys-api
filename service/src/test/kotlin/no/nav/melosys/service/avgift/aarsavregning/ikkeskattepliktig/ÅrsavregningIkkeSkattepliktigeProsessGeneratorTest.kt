package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ÅrsavregningIkkeSkattepliktigeProsessGeneratorTest {

    @MockK
    private lateinit var årsavregningIkkeSkattepliktigeFinner: ÅrsavregningIkkeSkattepliktigeFinner

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var generator: ÅrsavregningIkkeSkattepliktigeProsessGenerator

    @BeforeEach
    fun setUp() {
        generator = ÅrsavregningIkkeSkattepliktigeProsessGenerator(
            årsavregningIkkeSkattepliktigeFinner,
            prosessinstansService,
            behandlingsresultatService,
        )
    }

    @Test
    fun `hopper over når aktiv ÅRSAVREGNING for samme år finnes`() {
        val fagsak = lagFagsakMedÅrsavregning(behandlingId = AARSAVREGNING_BEHANDLING_ID)
        val årsavregningBehandling = fagsak.behandlinger.first { it.type == Behandlingstyper.ÅRSAVREGNING }

        every {
            årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(any(), any())
        } returns listOf(
            ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger(fagsak, listOf(årsavregningBehandling)),
        )

        every {
            behandlingsresultatService.hentBehandlingsresultat(AARSAVREGNING_BEHANDLING_ID)
        } returns Behandlingsresultat.forTest {
            id = AARSAVREGNING_BEHANDLING_ID
            årsavregning {
                aar = ÅR
            }
        }

        generator.finnSakerOgLagProsessinstanser(
            dryrun = false,
            antallFeilFørStopAvJob = 0,
            fomDato = FOM,
            tomDato = TOM,
        )

        verify(exactly = 0) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any())
        }
        val status = generator.status()
        status["antallHoppetOver"] shouldBe 1
        status["antallProsessert"] shouldBe 1
    }

    @Test
    fun `oppretter ny behandling når ingen aktiv ÅRSAVREGNING finnes`() {
        val fagsak = Fagsak.forTest {
            saksnummer = FagsakTestFactory.SAKSNUMMER
            type = Sakstyper.FTRL
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.LOVVALG_AVKLART
            // Ingen ÅRSAVREGNING-behandling — kun en avsluttet førstegangsbehandling
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
        }

        every {
            årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(any(), any())
        } returns listOf(
            ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger(fagsak, fagsak.behandlinger.toList()),
        )

        every {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any())
        } returns UUID.randomUUID()

        generator.finnSakerOgLagProsessinstanser(
            dryrun = false,
            antallFeilFørStopAvJob = 0,
            fomDato = FOM,
            tomDato = TOM,
        )

        verify(exactly = 1) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                fagsak.saksnummer,
                ÅR.toString(),
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE,
                true,
            )
        }
        generator.status()["antallHoppetOver"] shouldBe 0
    }

    @Test
    fun `oppretter ny behandling når aktiv ÅRSAVREGNING finnes for annet år`() {
        val fagsak = lagFagsakMedÅrsavregning(behandlingId = AARSAVREGNING_BEHANDLING_ID)
        val årsavregningBehandling = fagsak.behandlinger.first { it.type == Behandlingstyper.ÅRSAVREGNING }

        every {
            årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(any(), any())
        } returns listOf(
            ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger(fagsak, listOf(årsavregningBehandling)),
        )

        every {
            behandlingsresultatService.hentBehandlingsresultat(AARSAVREGNING_BEHANDLING_ID)
        } returns Behandlingsresultat.forTest {
            id = AARSAVREGNING_BEHANDLING_ID
            årsavregning {
                aar = ÅR - 1   // Eksisterende ÅRSAVREGNING gjelder forrige år
            }
        }

        every {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any())
        } returns UUID.randomUUID()

        generator.finnSakerOgLagProsessinstanser(
            dryrun = false,
            antallFeilFørStopAvJob = 0,
            fomDato = FOM,
            tomDato = TOM,
        )

        verify(exactly = 1) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                fagsak.saksnummer,
                ÅR.toString(),
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE,
                true,
            )
        }
        generator.status()["antallHoppetOver"] shouldBe 0
    }

    @Test
    fun `defensiv håndtering oppretter ny behandling når åpen ÅRSAVREGNING mangler årsavregning-rad (Bug 2)`() {
        val fagsak = lagFagsakMedÅrsavregning(behandlingId = AARSAVREGNING_BEHANDLING_ID)
        val årsavregningBehandling = fagsak.behandlinger.first { it.type == Behandlingstyper.ÅRSAVREGNING }

        every {
            årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(any(), any())
        } returns listOf(
            ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger(fagsak, listOf(årsavregningBehandling)),
        )

        // Behandlingsresultat uten årsavregning-rad — den ekte hentÅrsavregning() vil kaste
        // IllegalStateException ("årsavregning er påkrevd for Behandlingsresultat").
        // Speiler tilstanden vi ser i prod for MEL-436385, MEL-498817, MEL-559926.
        every {
            behandlingsresultatService.hentBehandlingsresultat(AARSAVREGNING_BEHANDLING_ID)
        } returns Behandlingsresultat.forTest {
            id = AARSAVREGNING_BEHANDLING_ID
            // Bevisst INGEN årsavregning satt — slik at hentÅrsavregning() kaster
        }

        every {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any())
        } returns UUID.randomUUID()

        // Skal IKKE kaste exception fra generatoren — runCatching håndterer den
        generator.finnSakerOgLagProsessinstanser(
            dryrun = false,
            antallFeilFørStopAvJob = 0,
            fomDato = FOM,
            tomDato = TOM,
        )

        // Per Yvonnes avklaring C: ny ÅRSAVREGNING SKAL opprettes når åpen mangler år
        // https://jira.adeo.no/browse/MELOSYS-8045
        verify(exactly = 1) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                fagsak.saksnummer,
                ÅR.toString(),
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE,
                true,
            )
        }
        generator.status()["antallHoppetOver"] shouldBe 0
    }

    private fun lagFagsakMedÅrsavregning(behandlingId: Long): Fagsak = Fagsak.forTest {
        saksnummer = FagsakTestFactory.SAKSNUMMER
        type = Sakstyper.FTRL
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.LOVVALG_AVKLART
        behandling {
            id = behandlingId
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.OPPRETTET
        }
    }

    companion object {
        const val ÅR = 2024
        val FOM: LocalDate = LocalDate.of(ÅR, 1, 1)
        val TOM: LocalDate = LocalDate.of(ÅR, 12, 31)
        const val AARSAVREGNING_BEHANDLING_ID = 100L
    }
}
