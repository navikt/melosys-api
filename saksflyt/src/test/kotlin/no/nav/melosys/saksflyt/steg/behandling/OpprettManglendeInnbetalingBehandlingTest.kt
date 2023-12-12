package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
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

    private lateinit var opprettManglendeInnbetalingBehandling: OpprettManglendeInnbetalingBehandling

    @BeforeEach
    fun setUp() {
        opprettManglendeInnbetalingBehandling =
            OpprettManglendeInnbetalingBehandling(behandlingService, behandlingsresultatService, saksbehandlingRegler)
    }

    @Test
    fun `inngangsSteg skal returnere OPPRETT_MANGLENDE_INNBETALING_BEHANDLING`() {
        opprettManglendeInnbetalingBehandling.inngangsSteg()
            .shouldBe(ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING)
    }

    @Test
    fun `utfør skal kaste feil dersom man ikke har behandlingsresultat med gitt fakturaserieReferanse`() {
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.FAKTURASERIE_REFERANSE, "referanse")
        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse("referanse") } returns emptyList()


        shouldThrow<FunksjonellException> { opprettManglendeInnbetalingBehandling.utfør(prosessinstans) }
            .message.shouldContain("Finner ikke behandlingsresultat med fakturaserie-referanse: referanse")
    }

    @Test
    fun `utfør skal kaste feil dersom man ikke har behandling som kan brukes til replikering`() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            fakturaserieReferanse = "referanse"
        }
        val behandling = Behandling().apply { fagsak = Fagsak() }
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.FAKTURASERIE_REFERANSE, behandlingsresultat.fakturaserieReferanse)
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
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            fakturaserieReferanse = "referanse"
        }
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
        }
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.FAKTURASERIE_REFERANSE, behandlingsresultat.fakturaserieReferanse)
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottaksdato)

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

}
