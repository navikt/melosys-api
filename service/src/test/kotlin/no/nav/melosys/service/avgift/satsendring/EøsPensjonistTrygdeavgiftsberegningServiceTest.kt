package no.nav.melosys.service.avgift.satsendring

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import no.nav.melosys.domain.kodeverk.Sakstemaer
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningResponse
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
import no.nav.melosys.service.avgift.EøsPensjonistTrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftperiodeErstatter
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class EøsPensjonistTrygdeavgiftsberegningServiceTest {
    @MockK
    private lateinit var mockBehandlingService: BehandlingService

    @MockK
    private lateinit var mockEregFasade: EregFasade

    @MockK
    private lateinit var mockTrygdeavgiftConsumer: TrygdeavgiftConsumer

    @MockK(relaxed = true)
    lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mockPersondataService: PersondataService

    @MockK
    private lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    private lateinit var trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var trygdeavgiftsberegningService: EøsPensjonistTrygdeavgiftsberegningService

    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat

    private val unleash: FakeUnleash = FakeUnleash()

    private val FOM: LocalDate = LocalDate.now()
    private val TOM: LocalDate = LocalDate.now().plusMonths(2)
    private val BEHANDLING_ID: Long = 1L
    private val FULLMEKTIG_AKTØR_ID: String = "123456789"
    private val FULLMEKTIG_NAVN: String = "Herr Fullmektig"
    private val FULLMEKTIG_ORGNR: String = "888888888"
    private val FULLMEKTIG_ORG_NAVN: String = "Aksjeselskap AS"
    private val BRUKER_NAVN: String = "Bruker Etternavn"
    private val FØDSELSDATO: LocalDate = LocalDate.of(2020, 1, 1)


    @BeforeEach
    fun setup() {
        unleash.enableAll()
        trygdeavgiftperiodeErstatter = spyk(TrygdeavgiftperiodeErstatter(mockBehandlingsresultatService))
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
        trygdeavgiftsberegningService = EøsPensjonistTrygdeavgiftsberegningService(
            mockBehandlingService,
            mockEregFasade,
            mockBehandlingsresultatService,
            trygdeavgiftperiodeErstatter,
            trygdeavgiftMottakerService,
            helseutgiftDekkesPeriodeService,
            mockPersondataService,
            mockTrygdeavgiftConsumer,
            unleash
        )
        behandling = Behandling().apply {
            id = BEHANDLING_ID
            tema = Behandlingstema.PENSJONIST
            type = Behandlingstyper.FØRSTEGANG
            fagsak = FagsakTestFactory.builder()
                .tema(Sakstemaer.TRYGDEAVGIFT)
                .medBruker()
                .build()
        }

        behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            behandling = this@EøsPensjonistTrygdeavgiftsberegningServiceTest.behandling
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }


        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIG_AKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
    }

    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `Beregner og lagrer trygdeavgift for EØS pensjonist`() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        val helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            fomDato = FOM,
            tomDato = TOM,
            bostedLandkode = Land_iso2.DK,
            behandlingsresultat = behandlingsresultat,
        )

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT
                isArbeidsgiversavgiftBetalesTilSkatt = false
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
            }
        )


        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(BEHANDLING_ID) }.returns(helseutgiftDekkesPeriode)
        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        DatoPeriodeDto(FOM, TOM),
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
            .shouldNotBeNull().shouldNotBeEmpty()

        verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_inntekstperioderDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skatteforholdTilNorgeDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                Skatteplikttype.SKATTEPLIKTIG

            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

}
