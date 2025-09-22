package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringIntervall
import no.nav.melosys.saksflyt.steg.fakturering.OpprettFakturaserie
import no.nav.melosys.saksflyt.steg.fakturering.OpprettFakturaserie.Companion.DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
class OpprettFakturaserieTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    @RelaxedMockK
    lateinit var pdlService: PersondataService

    @RelaxedMockK
    lateinit var trygdeavgiftService: TrygdeavgiftService

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private val slotFakturaserieDto = slot<FakturaserieDto>()

    private lateinit var opprettFakturaserie: OpprettFakturaserie

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak
    private lateinit var prosessinstans: Prosessinstans
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    internal fun setUp() {
        slotFakturaserieDto.clear()
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(behandlingsresultatService)

        opprettFakturaserie = OpprettFakturaserie(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            pdlService,
            trygdeavgiftService
        )
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier`() {
        lagTestData(setOf(lagAktoerBruker()))
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier for eøs pensjonister når inntektskilde type er PENSJON`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling = Behandling.forTest {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest {
                    aktører(setOf(lagAktoerBruker()))
                    betalingsvalg(Betalingstype.FAKTURA)
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
        }

        behandlingsresultat.apply {
            helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(this).apply {
                trygdeavgiftsperioder = mutableSetOf(
                    lagTrygdeavgift()
                )
            }
        }

        behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.forEach {
            it.grunnlagInntekstperiode!!.type = Inntektskildetype.PENSJON
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Informasjon om trygdeavgift datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldNotContain("Dekning")
            perioder.single().beskrivelse.shouldContain("Inntekt: 5000.0")
            perioder.single().beskrivelse.shouldContain("Sats: 3.5 %")
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier for eøs pensjonister - en periode skal ikke forskuddsfaktureres`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling = Behandling.forTest {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest {
                    aktører(setOf(lagAktoerBruker()))
                    betalingsvalg(Betalingstype.FAKTURA)
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
        }

        behandlingsresultat.apply {
            helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(this).apply {
                trygdeavgiftsperioder = mutableSetOf(
                    lagTrygdeavgift(),
                    lagTrygdeavgift().copyEntity(
                        periodeFra = LocalDate.of(2023, 5, 2),
                        periodeTil = LocalDate.of(2023, 12, 31),
                        skalForskuddsvisFaktureres = false
                    )
                )
            }
        }

        behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.forEach {
            it.grunnlagInntekstperiode!!.type = Inntektskildetype.PENSJON
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Informasjon om trygdeavgift datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldNotContain("Dekning")
            perioder.single().beskrivelse.shouldContain("Inntekt: 5000.0")
            perioder.single().beskrivelse.shouldContain("Sats: 3.5 %")
        }
    }

    @Test
    fun `Ikke opprett faktura da ingen trygdeavgiftsperioder skal forskuddsfaktureres`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling = Behandling.forTest {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest {
                    aktører(setOf(lagAktoerBruker()))
                    betalingsvalg(Betalingstype.FAKTURA)
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
        }

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 5, 1),
            trygdeavgiftsbeløpMd = Penger(5000.0),
            trygdesats = BigDecimal(3.5),
            grunnlagInntekstperiode = lagInntektsperiode(),
            grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge(),
            forskuddsvisFaktura = false
        )

        behandlingsresultat.apply {
            medlemskapsperioder = setOf(
                Medlemskapsperiode().apply {
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    fom = LocalDate.of(2022, 1, 1)
                    tom = LocalDate.of(2023, 5, 31)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                    trygdeavgiftsperioder = setOf(
                        trygdeavgiftsperiode
                    )
                }
            )
        }


        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify { faktureringskomponentenConsumer wasNot Called }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier når Inntektskildetype er PENSJON_UFØRETRYGD`() {
        lagTestData(setOf(lagAktoerBruker()))
        behandlingsresultat.trygdeavgiftsperioder.forEach {
            it.grunnlagInntekstperiode!!.type = Inntektskildetype.PENSJON_UFØRETRYGD
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldContain("Dekning: $DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL")
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier når Inntektskildetype er PENSJON_UFØRETRYGD_KILDESKATT`() {
        lagTestData(setOf(lagAktoerBruker()))
        behandlingsresultat.trygdeavgiftsperioder.forEach {
            it.grunnlagInntekstperiode!!.type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldContain("Dekning: $DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL")
        }
    }

    @Test
    fun `Kanseller betaling når resultat er opphørt`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandlingsresultat.type = Behandlingsresultattyper.OPPHØRT
            behandlingsresultat.fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling.opprinneligBehandling = Behandling.forTest { id = OPPRINNELIG_BEHANDLING_ID }

        }
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = behandlingsresultat.fakturaserieReferanse }
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, BRUKER_AKTØRID) } returns NyFakturaserieResponseDto(
            FAKTURASERIE_REFERANSE
        )


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Kanseller betaling når manglende innbetaling resulterer i fjerning av trygdeavgift`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = Behandling.forTest { id = OPPRINNELIG_BEHANDLING_ID }
            behandlingsresultat.type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandlingsresultat.fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandlingsresultat.trygdeavgiftsperioder.first().apply {
                grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt = true
                grunnlagSkatteforholdTilNorge!!.skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = behandlingsresultat.fakturaserieReferanse }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, BRUKER_AKTØRID) } returns
            NyFakturaserieResponseDto(FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Kanseller betaling når ny vurdering resulterer i fjerning av trygdeavgift`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling.forTest { id = OPPRINNELIG_BEHANDLING_ID }
            behandlingsresultat.type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandlingsresultat.fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandlingsresultat.trygdeavgiftsperioder.first().apply {
                grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt = true
                grunnlagSkatteforholdTilNorge!!.skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = behandlingsresultat.fakturaserieReferanse }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, BRUKER_AKTØRID) } returns
            NyFakturaserieResponseDto(FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Ikke kanseller betaling når resultat er ny vurdering og trygdeavgift betales til NAV`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandlingsresultat.vedtakMetadata.vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            behandling.type = Behandlingstyper.NY_VURDERING
            behandlingsresultat.fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling.opprinneligBehandling = lagBehandling(fagsak).apply { id = OPPRINNELIG_BEHANDLING_ID }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val opprinneligBehandlingsresultat = lagBehandlingsresultat(behandling).apply { fakturaserieReferanse = FAKTURASERIE_REFERANSE }
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Opprett betalingsplan for ny vurdering`() {
        lagTestData(setOf(lagAktoerBruker()))
        val eldsteBehandling = Behandling.forTest {
            id = 0L
            registrertDato = Instant.MIN
            status = Behandlingsstatus.AVSLUTTET
        }
        val tidligereBehandling = Behandling.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            registrertDato = Instant.EPOCH
            status = Behandlingsstatus.AVSLUTTET
        }
        fagsak.behandlinger.addAll(listOf(eldsteBehandling, tidligereBehandling))
        val tidligereBehandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = "3456" }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns tidligereBehandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fakturaserieReferanse.shouldBe(tidligereBehandlingsresultat.fakturaserieReferanse)
    }

    @Test
    fun `Opprett betalingsplan kun for trygdeavgiftsperioder med avgift`() {
        lagTestData(setOf(lagAktoerBruker()))
        behandlingsresultat.medlemskapsperioder.first().apply {
            this.trygdeavgiftsperioder = setOf(
                this.trygdeavgiftsperioder.first(),
                lagTrygdeavgift().copyEntity(trygdeavgiftsbeløpMd = Penger(0.0), trygdesats = BigDecimal(0))
            )
        }

        behandlingsresultat.trygdeavgiftsperioder.size.shouldBe(2)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.perioder.size.shouldBe(1)
    }

    @Test
    fun `Ikke opprett betalingsplan når behandling ikke har trygdeavgiftsperioder med avgift`() {
        lagTestData(emptySet())
        behandlingsresultat.medlemskapsperioder.first().apply {
            this.trygdeavgiftsperioder = setOf(lagTrygdeavgift().copyEntity(trygdeavgiftsbeløpMd = Penger(0.0), trygdesats = BigDecimal(0)))
        }

        behandlingsresultat.trygdeavgiftsperioder.size.shouldBe(1)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.lagFakturaserie(any(), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Ikke opprett betalingsplan når trygdeavgift betales til skatt`() {
        lagTestData(emptySet())
        behandlingsresultat.hentInntektsperioder().first()
            .apply {
                isArbeidsgiversavgiftBetalesTilSkatt = true
            }
        behandlingsresultat.hentSkatteforholdTilNorge().first()
            .apply {
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.lagFakturaserie(any(), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Opprett betalingsplan med organisasjon-fullmektig sender fullmektig`() {
        lagTestData(setOf(lagFullmektig().apply { orgnr = FULLMEKTIG_IDENT }, lagAktoerBruker()))
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fullmektig?.organisasjonsnummer.shouldBe(FULLMEKTIG_IDENT)
        slotFakturaserieDto.captured.fullmektig?.fodselsnummer.shouldBeNull()
    }

    @Test
    fun `Opprett betalingsplan med person-fullmektig sender fullmektig`() {
        lagTestData(setOf(lagFullmektig().apply { personIdent = FULLMEKTIG_IDENT }, lagAktoerBruker()))
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fullmektig?.fodselsnummer.shouldBe(FULLMEKTIG_IDENT)
        slotFakturaserieDto.captured.fullmektig?.organisasjonsnummer.shouldBeNull()
    }

    @Test
    fun `Opprett betalingsplan for pensjonister som ønsker faktura - BETALINGSVALG er FAKTURA`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling.apply {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest {
                    aktører(setOf(lagAktoerBruker()))
                    betalingsvalg = Betalingstype.FAKTURA
                }
            }
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Ikke opprett betalingsplan for pensjonister - BETALINGSVALG er TREKK`() {
        lagTestData(setOf(lagAktoerBruker())).apply {
            behandling.apply {
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest { betalingsvalg(Betalingstype.TREKK) }
            }
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify { faktureringskomponentenConsumer wasNot Called }
    }

    private fun lagTestData(aktører: Set<Aktoer>) {
        this.fagsak = Fagsak.forTest {
            aktører(aktører)
            betalingsvalg(Betalingstype.FAKTURA)
        }
        this.behandling = lagBehandling(fagsak)
        prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(this@OpprettFakturaserieTest.behandling)
        }
        behandlingsresultat = lagBehandlingsresultat(behandling)
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling {
        val behandling = Behandling.forTest()
        behandling.id = BEHANDLING_ID
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.fagsak = fagsak
        return behandling
    }

    private fun lagBehandlingsresultat(behandling: Behandling): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.behandling = behandling
        behandlingsresultat.id = 1L
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        vedtakMetadata.vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        behandlingsresultat.medlemskapsperioder = lagMedlemskapsperioder()
        return behandlingsresultat
    }

    private fun lagMedlemskapsperioder(): Set<Medlemskapsperiode> {
        val medlemskapsperiode = Medlemskapsperiode()
        return setOf(medlemskapsperiode.apply {
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2023, 5, 31)
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            trygdeavgiftsperioder = setOf(
                lagTrygdeavgift().copyEntity(grunnlagMedlemskapsperiode = medlemskapsperiode)
            )
        })
    }

    private fun lagHelseutgiftDekkesPeriode(behandlingsresultat: Behandlingsresultat): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = LocalDate.of(2023, 1, 1),
            tomDato = LocalDate.of(2023, 5, 1),
            bostedLandkode = Land_iso2.DK
        )
    }

    private fun lagTrygdeavgift(): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 5, 1),
            trygdeavgiftsbeløpMd = Penger(5000.0),
            trygdesats = BigDecimal(3.5),
            grunnlagInntekstperiode = lagInntektsperiode(),
            grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge()
        )
    }

    private fun lagInntektsperiode(): Inntektsperiode {
        return Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 5, 1)
            avgiftspliktigMndInntekt = Penger(5000.0)
        }
    }

    private fun lagSkatteforholdTilNorge(): SkatteforholdTilNorge {
        return SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2022, 1, 1)
            tomDato = LocalDate.of(2023, 5, 31)
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    }

    private fun lagFullmektig(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.setFullmaktstype(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        return aktoer
    }

    private fun lagAktoerBruker(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        aktoer.aktørId = BRUKER_FNR
        return aktoer
    }

    companion object {
        const val SAKSBEHANDLER_IDENT = "S123456"
        const val BEHANDLING_ID = 1L
        const val OPPRINNELIG_BEHANDLING_ID = 2L
        const val BRUKER_FNR = "11111111111"
        const val BRUKER_AKTØRID = "12345678911"
        const val FULLMEKTIG_IDENT = "123456789"
        const val FAKTURASERIE_REFERANSE = "1234"
    }
}
