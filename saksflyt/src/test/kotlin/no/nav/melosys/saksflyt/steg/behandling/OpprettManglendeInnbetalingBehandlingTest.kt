package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettManglendeInnbetalingBehandlingTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    private lateinit var oppgaveService: OppgaveService

    private lateinit var opprettManglendeInnbetalingBehandling: OpprettManglendeInnbetalingBehandling

    @BeforeEach
    fun setUp() {
        opprettManglendeInnbetalingBehandling =
            OpprettManglendeInnbetalingBehandling(behandlingService, behandlingsresultatService, saksbehandlingRegler, oppgaveService)
    }

    @Test
    fun `inngangsSteg skal returnere OPPRETT_MANGLENDE_INNBETALING_BEHANDLING`() {
        opprettManglendeInnbetalingBehandling.inngangsSteg()
            .shouldBe(ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING)
    }

    @Test
    fun `utfør skal kaste feil dersom man ikke har behandlingsresultat med gitt fakturaserieReferanse`() {
        val prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.FAKTURASERIE_REFERANSE, "referanse")
        }
        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse("referanse") } returns emptyList()


        shouldThrow<FunksjonellException> { opprettManglendeInnbetalingBehandling.utfør(prosessinstans) }
            .message.shouldContain("Finner ikke behandlingsresultat med fakturaserie-referanse: referanse")
    }

    @Test
    fun `utfør skal kaste feil dersom man ikke har behandling som kan brukes til replikering`() {
        val behandlingsresultat = lagBehandlingsresultat()
        val behandling = Behandling().apply { fagsak = Fagsak() }
        val prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.FAKTURASERIE_REFERANSE, behandlingsresultat.fakturaserieReferanse)
        }
        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse) } returns listOf(
            behandlingsresultat
        )
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns null


        shouldThrow<FunksjonellException> { opprettManglendeInnbetalingBehandling.utfør(prosessinstans) }
            .message.shouldContain("Finner ikke behandling som skal brukes til replikering")
    }

    @Test
    fun `utfør skal replikere behandling og sette rette verdier`() {
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat()

        val behandling = lagBehandling {
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
        }

        val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)

        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse) } returns listOf(
            behandlingsresultat
        )
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns behandling
        every {
            behandlingService.replikerBehandlingOgBehandlingsresultat(
                behandling,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            )
        } returnsArgument 0


        opprettManglendeInnbetalingBehandling.utfør(prosessinstans)


        verify { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) }
        prosessinstans.behandling.shouldNotBeNull().run {
            behandlingsårsak.type.shouldBe(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING)
            behandlingsårsak.mottaksdato.shouldBe(mottaksdato)
            behandlingsfrist.shouldBe(Behandling.utledBehandlingsfrist(this, mottaksdato))
        }
    }

    @Test
    fun `aktiv behandling med type MANGLENDE_INNBETALING_TRYGDEAVGIFT - utfør skal sette prosessinstans behandling til aktivBehandling og avslutte`() {
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat()
        val behandling = lagBehandling {
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            status = Behandlingsstatus.UNDER_BEHANDLING
        }

        val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)


        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse)
        } returns listOf(behandlingsresultat)
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns behandling
        every {
            behandlingService.replikerBehandlingOgBehandlingsresultat(
                behandling,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            )
        } returns behandling


        opprettManglendeInnbetalingBehandling.utfør(prosessinstans)


        verify(exactly = 0) { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, any()) }
        verify(exactly = 0) { saksbehandlingRegler.finnBehandlingSomKanReplikeres(any()) }

        prosessinstans.behandling.shouldNotBeNull().run {
            behandling.shouldBe(behandling)
            type.shouldBe(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            status.shouldBe(Behandlingsstatus.UNDER_BEHANDLING)
        }
    }

    @Test
    fun `aktiv behandling med type NY_VURDERING og en opprinneligBehandling - sett riktig type og ikke oppdater frist når mindre en 6 uker`() {
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat()
        val behandling = lagBehandling {
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            behandlingsfrist = LocalDate.now().plusWeeks(5)
            opprinneligBehandling = Behandling()
        }
        val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse)
        } returns listOf(behandlingsresultat)
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns behandling
        every {
            behandlingService.replikerBehandlingOgBehandlingsresultat(
                behandling,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            )
        } returns behandling


        opprettManglendeInnbetalingBehandling.utfør(prosessinstans)


        verify(exactly = 0) { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, any()) }
        verify(exactly = 0) { saksbehandlingRegler.finnBehandlingSomKanReplikeres(any()) }

        prosessinstans.behandling.shouldNotBeNull().run {
            behandling.shouldBe(behandling)
            behandlingsfrist.shouldBe(LocalDate.now().plusWeeks(5))
            type.shouldBe(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            status.shouldBe(Behandlingsstatus.UNDER_BEHANDLING)
        }
    }

    @Test
    fun `aktiv behandling med type NY_VURDERING og en opprinneligBehandling - sett riktig type og oppdater frist når mer en 6 uker`() {
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat()
        val behandling = lagBehandling {
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            behandlingsfrist = LocalDate.now().plusWeeks(7)
            opprinneligBehandling = Behandling()
        }
        val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse)
        } returns listOf(behandlingsresultat)
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns behandling
        every {
            behandlingService.replikerBehandlingOgBehandlingsresultat(
                behandling,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            )
        } returns behandling


        opprettManglendeInnbetalingBehandling.utfør(prosessinstans)


        verify(exactly = 0) { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, any()) }
        verify(exactly = 0) { saksbehandlingRegler.finnBehandlingSomKanReplikeres(any()) }

        prosessinstans.behandling.shouldNotBeNull().run {
            behandling.shouldBe(behandling)
            behandlingsfrist.shouldBe(LocalDate.now().plusWeeks(6))
            type.shouldBe(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            status.shouldBe(Behandlingsstatus.UNDER_BEHANDLING)
        }
    }

    @Test
    fun `aktiv behandling med type HENVENDELSE og NY_VURDERING og ikke replikert behandling`() {
        mapOf(
            "HENVENDELSE" to Behandlingstyper.HENVENDELSE,
            "NY_VURDERING" to Behandlingstyper.NY_VURDERING
        ).forEach { (beskrivelse, behandlingstype) ->
            clearMocks(behandlingService, saksbehandlingRegler, behandlingsresultatService, oppgaveService)
            withClue(beskrivelse) {

                val mottaksdato = LocalDate.now()
                val behandlingsresultat = lagBehandlingsresultat()
                val behandling = lagBehandling {
                    type = behandlingstype
                    status = Behandlingsstatus.UNDER_BEHANDLING
                }
                val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)

                every {
                    behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse)
                } returns listOf(behandlingsresultat)
                every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
                every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) } returns behandling
                every {
                    behandlingService.replikerBehandlingOgBehandlingsresultat(
                        behandling,
                        Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                    )
                } returns lagBehandling {
                    type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                    status = Behandlingsstatus.UNDER_BEHANDLING
                }
                every { behandlingService.avsluttBehandling(any()) } returns Unit
                every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } returns Unit
                every { oppgaveService.ferdigstillOppgaveMedSaksnummer(any()) } returns Unit


                opprettManglendeInnbetalingBehandling.utfør(prosessinstans)


                verify(exactly = 1) {
                    behandlingService.replikerBehandlingOgBehandlingsresultat(
                        behandling,
                        Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                    )
                }
                verify(exactly = 1) { saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandling.fagsak) }
                verify(exactly = 1) { behandlingService.avsluttBehandling(1L) }
                verify(exactly = 1) { behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.AVBRUTT) }
                verify(exactly = 1) { oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.fagsak.saksnummer) }

                prosessinstans.behandling.shouldNotBeNull().run {
                    behandling.shouldBe(behandling)
                    type.shouldBe(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
                    status.shouldBe(Behandlingsstatus.UNDER_BEHANDLING)
                    mottaksdato.shouldBe(mottaksdato)
                    behandlingsfrist.shouldBe(LocalDate.now().plusWeeks(6))
                }
            }
        }
    }

    @Test
    fun `kaster feil dersom man har aktiv behandling men ikke har støtte`() {
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat()
        val behandling = lagBehandling {
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        val prosessinstans = lagProsessinstans(behandlingsresultat.fakturaserieReferanse, mottaksdato)

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(behandlingsresultat.fakturaserieReferanse)
        } returns listOf(behandlingsresultat)
        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling


        shouldThrow<FunksjonellException> {
            opprettManglendeInnbetalingBehandling.utfør(prosessinstans)
        }.message.shouldBe("Har ikke støtte for aktiv behandling: 1")
    }

    private fun lagBehandlingsresultat() = Behandlingsresultat().apply {
        id = 1L
        fakturaserieReferanse = "referanse"
    }

    private fun lagProsessinstans(
        behandlingsresultat: Behandlingsresultat,
        mottaksdato: LocalDate?
    ) = Prosessinstans().apply {
        setData(ProsessDataKey.FAKTURASERIE_REFERANSE, behandlingsresultat.fakturaserieReferanse)
        setData(ProsessDataKey.MOTTATT_DATO, mottaksdato)
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = Behandling().apply behandling@{
        block()
        id = id ?: 1L
        fagsak = fagsak ?: Fagsak().apply {
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            type = Sakstyper.FTRL
            behandlinger.add(this@behandling)
        }
        tema = tema ?: Behandlingstema.YRKESAKTIV
    }
}
