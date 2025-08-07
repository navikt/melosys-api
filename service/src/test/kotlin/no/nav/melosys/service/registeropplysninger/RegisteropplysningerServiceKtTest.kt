package no.nav.melosys.service.registeropplysninger

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.inntekt.InntektService
import no.nav.melosys.integrasjon.utbetaling.UtbetaldataRestService
import no.nav.melosys.service.aareg.ArbeidsforholdService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@ExtendWith(MockitoExtension::class)
class RegisteropplysningerServiceKtTest {

    companion object {
        private const val FNR = "432234"
    }

    private val medlPeriodeService: MedlPeriodeService = mockk()
    private val eregFasade: EregFasade = mockk()
    private val arbeidsforholdService: ArbeidsforholdService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val inntektService: InntektService = mockk()
    private val saksopplysningerService: SaksopplysningerService = mockk()
    private val registeropplysningerPeriodeFactory: RegisteropplysningerPeriodeFactory = mockk()
    private val utbetaldataRestService: UtbetaldataRestService = mockk()

    private lateinit var registeropplysningerService: RegisteropplysningerService

    @BeforeEach
    fun setUp() {
        registeropplysningerService = RegisteropplysningerService(
            medlPeriodeService, eregFasade, arbeidsforholdService, behandlingService,
            inntektService, saksopplysningerService, registeropplysningerPeriodeFactory, utbetaldataRestService
        )

        every { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any<LocalDate>(), any<LocalDate>()) } returns lagSaksopplysning(SaksopplysningType.ARBFORH)
        every { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) } returns lagSaksopplysning(SaksopplysningType.MEDL)
        every { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) } returns lagSaksopplysning(SaksopplysningType.INNTK)
        every { eregFasade.hentOrganisasjon(any()) } returns lagSaksopplysning(SaksopplysningType.ORG)

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns hentBehandling()
        every { saksopplysningerService.finnArbeidsforholdsopplysninger(any()) } returns Optional.of(lagArbeidsforholdDokument())
        every { saksopplysningerService.finnInntektsopplysninger(any()) } returns Optional.empty()

        every { registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(any<LocalDate>(), any<LocalDate>()) } returns hentDatoPeriode()
        every { registeropplysningerPeriodeFactory.hentPeriodeForMedlemskap(any<LocalDate>(), any<LocalDate>(), any<Behandling>()) } returns hentDatoPeriode()
        every { registeropplysningerPeriodeFactory.hentPeriodeForInntekt(any<LocalDate>(), any<LocalDate>(), any<Behandling>()) } returns hentPeriode()

        every { utbetaldataRestService.hentUtbetalingerBarnetrygd(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.UTBETAL)

        every { behandlingService.lagre(any<Behandling>()) } just Runs
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger - alle blir hentet og lagret`() {
        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(
                    RegisteropplysningerRequest.SaksopplysningTyper.builder()
                        .arbeidsforholdopplysninger()
                        .inntektsopplysninger()
                        .medlemskapsopplysninger()
                        .organisasjonsopplysninger()
                        .utbetalingsopplysninger()
                        .build()
                )
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build()
        )

        verify { behandlingService.lagre(any<Behandling>()) }
        verify { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any<LocalDate>(), any<LocalDate>()) }
        verify { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) }
        verify { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) }
        verify { eregFasade.hentOrganisasjon(any()) }
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger i vilkårlig rekkefølge - alle blir hentet og lagret i rett rekkefølge`() {
        val arbeidsforhold = Arbeidsforhold().apply {
            arbeidsgiverID = "123456789"
        }

        val arbeidsforholdDokument = ArbeidsforholdDokument(listOf(arbeidsforhold))
        val saksopplysning = Saksopplysning().apply {
            dokument = arbeidsforholdDokument
            type = SaksopplysningType.ARBFORH
            leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.AAREG, null)
        }
        
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns hentBehandling(saksopplysning)

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.hentSaksopplysningTyperSomLagres())
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build()
        )

        verify { behandlingService.lagre(any<Behandling>()) }

        // Noen av stegene er avhengige av hverandre. Det er viktig at vi ivaretar rekkefølgen.
        verifyOrder {
            inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>())
            eregFasade.hentOrganisasjon(any())
        }

        verifyOrder {
            arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any<LocalDate>(), any<LocalDate>())
            eregFasade.hentOrganisasjon(any())
        }

        verify { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) }
    }

    @Test
    fun `hentArbeidsforholdopplysninger`() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()
        every { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any<LocalDate>(), any<LocalDate>()) } returns lagSaksopplysning(SaksopplysningType.ARBFORH)

        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
                .build()
        )

        verify { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(eq(FNR), any<LocalDate>(), any<LocalDate>()) }
        verify { behandlingService.lagre(any<Behandling>()) }
    }

    @Test
    fun `hentMedlemskapsopplysninger`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)
        val saksopplysning = hentSedSaksopplysning(fom, tom)
        every { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) } returns saksopplysning

        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().medlemskapsopplysninger().build())
                .build()
        )

        verify { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) }
        verify { behandlingService.lagre(any<Behandling>()) }
    }

    @Test
    fun `hentInntektsopplysninger`() {
        val fom = LocalDate.now().minusYears(3)
        val tom = LocalDate.now().minusYears(2)
        val saksopplysning = hentSedSaksopplysning(fom, tom)
        every { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) } returns saksopplysning

        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
                .build()
        )

        verify { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) }
        verify { behandlingService.lagre(any<Behandling>()) }
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger - skal hente 5 år før fom`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.hentSaksopplysningTyperSomLagres())
                .fom(fom)
                .tom(tom)
                .fnr(FNR)
                .hentOpplysningerFor5aar(true)
                .build()
        )

        verify { registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(fom.minusYears(5), tom) }
    }

    @Test
    fun `hentOgLagreOpplysninger feil i periode - kan ikke hente opplysninger som bruker periode`() {
        val fom = LocalDate.now().plusYears(2)
        val tom = LocalDate.now()

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.hentSaksopplysningTyperSomLagres())
                .fnr(FNR)
                .fom(fom)
                .tom(tom)
                .build()
        )

        verify(exactly = 0) { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any<LocalDate>(), any<LocalDate>()) }
        verify(exactly = 0) { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) }
        verify(exactly = 0) { medlPeriodeService.hentPeriodeListe(any(), any<LocalDate>(), any<LocalDate>()) }

        verify { eregFasade.hentOrganisasjon(any()) }
        verify { behandlingService.lagre(any<Behandling>()) }
    }

    private fun hentBehandling(): Behandling {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .medTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            .build()
    }

    private fun hentBehandling(saksopplysning: Saksopplysning): Behandling {
        val behandling = hentBehandling()
        behandling.saksopplysninger.add(saksopplysning)
        return behandling
    }

    private fun hentSedSaksopplysning(fom: LocalDate, tom: LocalDate): Saksopplysning {
        return Saksopplysning().apply {
            dokument = hentSedDokument(fom, tom)
            type = SaksopplysningType.SEDOPPL
        }
    }

    private fun hentSedDokument(fom: LocalDate, tom: LocalDate): SedDokument {
        return SedDokument().apply {
            lovvalgsperiode = no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom)
            fnr = "123"
        }
    }

    private fun lagSaksopplysning(saksopplysningType: SaksopplysningType): Saksopplysning {
        return Saksopplysning().apply {
            type = saksopplysningType
        }
    }

    private fun lagArbeidsforholdDokument(): ArbeidsforholdDokument {
        val arbeidsforhold = Arbeidsforhold().apply {
            arbeidsgiverID = "123456789"
        }

        val arbeidsforholdDokument = ArbeidsforholdDokument(listOf(arbeidsforhold))
        val saksopplysning = Saksopplysning().apply {
            dokument = arbeidsforholdDokument
            type = SaksopplysningType.ARBFORH
            leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.AAREG, null)
        }

        return arbeidsforholdDokument
    }

    private fun registeropplysningerRequest(fom: LocalDate, tom: LocalDate): RegisteropplysningerRequest.RegisteropplysningerRequestBuilder {
        return RegisteropplysningerRequest.builder()
            .behandlingID(2L)
            .fom(fom)
            .tom(tom)
            .fnr(FNR)
    }

    private fun saksopplysningstyper(): RegisteropplysningerRequest.SaksopplysningTyper.SaksopplysningTyperBuilder {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
    }

    private fun hentPeriode(): RegisteropplysningerPeriodeFactory.Periode {
        return RegisteropplysningerPeriodeFactory.Periode(YearMonth.now(), YearMonth.now())
    }

    private fun hentDatoPeriode(): RegisteropplysningerPeriodeFactory.DatoPeriode {
        return RegisteropplysningerPeriodeFactory.DatoPeriode(LocalDate.now(), LocalDate.now())
    }
}