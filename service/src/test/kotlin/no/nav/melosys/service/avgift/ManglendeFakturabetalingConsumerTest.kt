package no.nav.melosys.service.avgift

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
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
    fun `opprett prosess for varselbrev om manglende innbetaling når alle lovvalgsperioder er pliktige`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                }
            }
            lovvalgsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
            }
        }
        val behandling = behandlingsresultat.behandling

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


        manglendeFakturabetalingConsumer.lesManglendeFakturabetalingMelding(
            ConsumerRecord(
                "topic", 1, 1, "key", melding
            )
        )


        verify { prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding) }
        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingBehandling(any()) }
    }

    @Test
    fun `opprett prosess for varselbrev om manglende innbetaling når alle medlemskapsperioder er pliktige`() {
        // Given
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.PLIKTIG
            }
        }
        val behandling = behandlingsresultat.behandling

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
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                }
            }
        }
        val behandling = behandlingsresultat.behandling

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
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123
            }
            medlemskapsperiode {
                medlemskapstype = Medlemskapstyper.FRIVILLIG
            }
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

    @ParameterizedTest
    @EnumSource(
        value = Saksstatuser::class,
        names = ["ANNULLERT", "OPPHØRT"]
    )
    fun `skal ikke opprette prosess for manglende innbetaling eller varselbrev når fagsak er annullert eller opphørt`(fagsakStatus: Saksstatuser) {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    status = fagsakStatus
                }
            }
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

        manglendeFakturabetalingConsumer.lesManglendeFakturabetalingMelding(
            ConsumerRecord(
                "topic", 1, 1, "key", melding
            )
        )

        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(any(), any()) }
        verify(exactly = 0) { prosessinstansService.opprettProsessManglendeInnbetalingBehandling(melding) }
    }

    companion object {
        const val FAKTURASERIE_REFERANSE = "FS123456"
        const val FAKTURANUMMER = "F123456"
    }
}
