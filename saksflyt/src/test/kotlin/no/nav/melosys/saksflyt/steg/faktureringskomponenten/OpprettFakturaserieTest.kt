package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.getunleash.FakeUnleash
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall
import no.nav.melosys.saksflyt.steg.fakturering.OpprettFakturaserie
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
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

    lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private val unleash = FakeUnleash()
    private val slotFakturaserieDto = slot<FakturaserieDto>()

    lateinit var opprettFakturaserie: OpprettFakturaserie

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak
    private lateinit var prosessinstans: Prosessinstans
    private lateinit var fastsattTrygdeavgift: FastsattTrygdeavgift
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    internal fun setUp() {
        unleash.enableAll()
        slotFakturaserieDto.clear()
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService()

        opprettFakturaserie = OpprettFakturaserie(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            pdlService,
            trygdeavgiftMottakerService,
            unleash
        )
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier`() {
        lagTestData(setOf(lagAktoerBruker()))
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.referanseBruker.shouldContain("Vedtak om medlemskap datert ")
        slotFakturaserieDto.captured.fakturaserieReferanse.shouldBeNull()
    }

    @Test
    fun `Opprett betalingsplan for ny vurdering`() {
        val OPPRINNELIG_BEHANDLING_ID = 2L
        lagTestData(setOf(lagAktoerBruker()))
        behandling.opprinneligBehandling = Behandling().apply { id = OPPRINNELIG_BEHANDLING_ID }
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = "3456"}
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fakturaserieReferanse.shouldBe(opprinneligBehandlingsresultat.fakturaserieReferanse)
    }

    @Test
    fun `Opprett betalingsplan kun for trygdeavgiftsperioder med avgift`() {
        lagTestData(setOf(lagAktoerBruker()))
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.apply {
            this.trygdeavgiftsperioder.add(lagTrygdeavgift(behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift).apply {
                this.trygdeavgiftsbeløpMd = Penger(0.0)
                this.trygdesats = BigDecimal(0)
            })
        }

        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.size.shouldBe(2)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.perioder.size.shouldBe(1)
    }

    @Test
    fun `Ikke opprett betalingsplan når behandling ikke har trygdeavgiftsperioder med avgift`() {
        lagTestData(emptySet())
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.apply {
            this.trygdeavgiftsperioder =
                setOf(lagTrygdeavgift(behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift).apply {
                    this.trygdeavgiftsbeløpMd = Penger(0.0)
                    this.trygdesats = BigDecimal(0)
                })
        }

        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.size.shouldBe(1)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.lagFakturaserie(any(), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Ikke opprett betalingsplan når trygdeavgift betales til skatt`() {
        lagTestData(emptySet())
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.first()
            .apply {
                isArbeidsgiversavgiftBetalesTilSkatt = true
            }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.first()
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
        every { pdlService.finnFolkeregisterident(BRUKER_FNR) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fullmektig?.fodselsnummer.shouldBe(FULLMEKTIG_IDENT)
        slotFakturaserieDto.captured.fullmektig?.organisasjonsnummer.shouldBeNull()
    }

    private fun lagTestData(aktører: Set<Aktoer>) {
        this.fagsak = lagFagsak().apply { this.aktører = aktører }
        this.behandling = lagBehandling(fagsak)
        prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            setData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall.KVARTAL)
            this.behandling = this@OpprettFakturaserieTest.behandling
        }
        behandlingsresultat = lagBehandlingsresultat()
        fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift
    }

    private fun lagBehandling(fagsak: Fagsak = lagFagsak()): Behandling {
        val behandling = Behandling()
        behandling.id = BEHANDLING_ID
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.fagsak = fagsak
        return behandling
    }

    private fun lagFagsak(): Fagsak = Fagsak().apply {
        saksnummer = "MEL-100"
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.OPPRETTET
    }

    fun lagBehandlingsresultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 1L
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        vedtakMetadata.vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        behandlingsresultat.medlemAvFolketrygden = lagMedlemAvFolketrygden()
        return behandlingsresultat
    }

    private fun lagMedlemAvFolketrygden(): MedlemAvFolketrygden {
        return MedlemAvFolketrygden().apply {
            medlemskapsperioder = lagMedlemskapsperioder()
            fastsattTrygdeavgift = lagFastsattTrygdeavgift()
            fastsattTrygdeavgift.trygdeavgiftsperioder.first().grunnlagMedlemskapsperiode =
                medlemskapsperioder.first()
            fastsattTrygdeavgift.trygdeavgiftsperioder.first().grunnlagInntekstperiode =
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.first()
        }
    }

    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode> {
        return listOf(Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2023, 5, 31)
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        })
    }

    private fun lagFastsattTrygdeavgift(): FastsattTrygdeavgift {
        return FastsattTrygdeavgift().apply {
            trygdeavgiftsperioder = mutableSetOf(lagTrygdeavgift(this))
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = listOf(lagInntektsperiode())
                skatteforholdTilNorge = listOf(lagSkatteforholdTilNorge())
            }
        }
    }

    private fun lagTrygdeavgift(fastsattTrygdeavgift: FastsattTrygdeavgift): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode().apply {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 5, 1)
            trygdeavgiftsbeløpMd = Penger(5000.0)
            trygdesats = BigDecimal(3.5)
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
        }
    }

    private fun lagInntektsperiode(): Inntektsperiode {
        return Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 5, 1)
            avgiftspliktigInntektMnd = Penger(5000.0)
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
        const val BRUKER_FNR = "11111111111"
        const val BRUKER_AKTØRID = "12345678911"
        const val FULLMEKTIG_IDENT = "123456789"
    }
}
