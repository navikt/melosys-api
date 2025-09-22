package no.nav.melosys.service.avgift

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class ManglendeFakturabetalingConsumerTest {

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var manglendeFakturabetalingConsumer: ManglendeFakturabetalingConsumer

    @BeforeEach
    fun setUp() {
        manglendeFakturabetalingConsumer = ManglendeFakturabetalingConsumer(
            prosessinstansService,
            behandlingsresultatService
        )
    }

    @Test
    fun `opprett prosess for varselbrev om manglende innbetaling når alle medlemskapsperioder er pliktige`() {
        // Given
        val behandling = Behandling.forTest {
            id = 123
        }

        val medlemskapsperiode = mockk<Medlemskapsperiode>()
        every { medlemskapsperiode.erPliktig() } returns true

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            this.medlemskapsperioder = listOf(medlemskapsperiode)
        }

        val melding = ManglendeFakturabetalingMelding(
            fakturaserieReferanse = FAKTURASERIE_REFERANSE,
            betalingsstatus = Betalingsstatus.IKKE_BETALT,
            datoMottatt = LocalDate.now(),
            fakturanummer = FAKTURANUMMER
        )

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(FAKTURASERIE_REFERANSE)
        } returns listOf(behandlingsresultat)

        every {
            prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding)
        } returns mockk<UUID>()

        // When
        manglendeFakturabetalingConsumer.lesManglendeFakturabetalingMelding(
            ConsumerRecord(
                "topic", 1, 1, "key", melding
            )
        )

        // Then
        verify { prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding) }
        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingBehandling(any()) }
    }

    @Test
    fun `opprett prosess for varselbrev om manglende innbetaling når det er eøs pensjonist`() {
        // Given
        val behandling = Behandling.forTest {
            id = 123
            tema = Behandlingstema.PENSJONIST
            fagsak = Fagsak.forTest {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
        }

        val melding = ManglendeFakturabetalingMelding(
            fakturaserieReferanse = FAKTURASERIE_REFERANSE,
            betalingsstatus = Betalingsstatus.IKKE_BETALT,
            datoMottatt = LocalDate.now(),
            fakturanummer = FAKTURANUMMER
        )

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(FAKTURASERIE_REFERANSE)
        } returns listOf(behandlingsresultat)

        every {
            prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding)
        } returns mockk<UUID>()

        // When
        manglendeFakturabetalingConsumer.lesManglendeFakturabetalingMelding(
            ConsumerRecord(
                "topic", 1, 1, "key", melding
            )
        )

        // Then
        verify { prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding) }
        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingBehandling(any()) }
    }


    @Test
    fun `opprett prosess for manglende innbetaling behandling, frivillig medlemskap`() {
        // Given
        val behandling = Behandling.forTest {
            id = 123
        }

        val medlemskapsperiode = mockk<Medlemskapsperiode>()
        every { medlemskapsperiode.erPliktig() } returns false

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            this.medlemskapsperioder = listOf(medlemskapsperiode)
        }

        val melding = ManglendeFakturabetalingMelding(
            fakturaserieReferanse = FAKTURASERIE_REFERANSE,
            betalingsstatus = Betalingsstatus.IKKE_BETALT,
            datoMottatt = LocalDate.now(),
            fakturanummer = FAKTURANUMMER
        )

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(FAKTURASERIE_REFERANSE)
        } returns listOf(behandlingsresultat)

        every {
            prosessinstansService.opprettProsessManglendeInnbetalingBehandling(melding)
        } returns mockk<UUID>()

        // When
        manglendeFakturabetalingConsumer.lesManglendeFakturabetalingMelding(
            ConsumerRecord(
                "topic", 1, 1, "key", melding
            )
        )

        // Then
        verify { prosessinstansService.opprettProsessManglendeInnbetalingBehandling(melding) }
        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(any(), any()) }
    }

    companion object {
        const val FAKTURASERIE_REFERANSE = "FS123456"
        const val FAKTURANUMMER = "F123456"
    }
}
