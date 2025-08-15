package no.nav.melosys.service.registeropplysninger

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@ExtendWith(MockKExtension::class)
class RegisteropplysningerServiceKtTest {

    @MockK
    private lateinit var medlPeriodeService: MedlPeriodeService

    @MockK
    private lateinit var eregFasade: EregFasade

    @MockK
    private lateinit var arbeidsforholdService: ArbeidsforholdService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var inntektService: InntektService

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    private lateinit var registeropplysningerPeriodeFactory: RegisteropplysningerPeriodeFactory

    @MockK
    private lateinit var utbetaldataRestService: UtbetaldataRestService

    private lateinit var registeropplysningerService: RegisteropplysningerService

    @BeforeEach
    fun setUp() {
        registeropplysningerService = RegisteropplysningerService(
            medlPeriodeService,
            eregFasade,
            arbeidsforholdService,
            behandlingService,
            inntektService,
            saksopplysningerService,
            registeropplysningerPeriodeFactory,
            utbetaldataRestService
        )

        every { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.ARBFORH)
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.MEDL)
        every { inntektService.hentInntektListe(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.INNTK)
        every { eregFasade.hentOrganisasjon(any()) } returns lagSaksopplysning(SaksopplysningType.ORG)

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns hentBehandling()
        every { saksopplysningerService.finnArbeidsforholdsopplysninger(any()) } returns Optional.of(lagArbeidsforholdDokument())
        every { saksopplysningerService.finnInntektsopplysninger(any()) } returns Optional.empty()

        every { registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(any(), any()) } returns hentDatoPeriode()
        every { registeropplysningerPeriodeFactory.hentPeriodeForMedlemskap(any(), any(), any()) } returns hentDatoPeriode()
        every { registeropplysningerPeriodeFactory.hentPeriodeForInntekt(any(), any(), any()) } returns hentPeriode()

        every { behandlingService.lagre(any()) } returns mockk()
        every { utbetaldataRestService.hentUtbetalingerBarnetrygd(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.UTBETAL)
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger alle blir hentet og lagret`() {
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


        verify { behandlingService.lagre(any()) }
        verify { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any(), any()) }
        verify { inntektService.hentInntektListe(any(), any(), any()) }
        verify { medlPeriodeService.hentPeriodeListe(any(), any(), any()) }
        verify { eregFasade.hentOrganisasjon(any()) }
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger i vilkårlig rekkefølge alle blir hentet og lagret i rett rekkefølge`() {
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


        verify { behandlingService.lagre(any()) }

        // Verifiser rekkefølge
        verifyOrder {
            inntektService.hentInntektListe(any(), any(), any())
            eregFasade.hentOrganisasjon(any())
        }

        verifyOrder {
            arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any(), any())
            eregFasade.hentOrganisasjon(any())
        }

        verify { medlPeriodeService.hentPeriodeListe(any(), any(), any()) }
    }

    @Test
    fun `hentArbeidsforholdopplysninger henter og lagrer arbeidsforhold`() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()
        every { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any(), any()) } returns lagSaksopplysning(SaksopplysningType.ARBFORH)


        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
                .build()
        )


        verify { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(eq(FNR), any(), any()) }
        verify { behandlingService.lagre(any()) }
    }

    @Test
    fun `hentMedlemskapsopplysninger henter og lagrer medlemskap`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)
        val saksopplysning = hentSedSaksopplysning(fom, tom)
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns saksopplysning


        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().medlemskapsopplysninger().build())
                .build()
        )


        verify { medlPeriodeService.hentPeriodeListe(any(), any(), any()) }
        verify { behandlingService.lagre(any()) }
    }

    @Test
    fun `hentInntektsopplysninger henter og lagrer inntekt`() {
        val fom = LocalDate.now().minusYears(3)
        val tom = LocalDate.now().minusYears(2)
        val saksopplysning = hentSedSaksopplysning(fom, tom)
        every { inntektService.hentInntektListe(any(), any(), any()) } returns saksopplysning


        registeropplysningerService.hentOgLagreOpplysninger(
            registeropplysningerRequest(fom, tom)
                .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
                .build()
        )


        verify { inntektService.hentInntektListe(any(), any<YearMonth>(), any<YearMonth>()) }
        verify { behandlingService.lagre(any()) }
    }

    @Test
    fun `hentOgLagreOpplysninger med alle opplysninger skal hente 5 år før fom`() {
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
    fun `hentOgLagreOpplysninger feil i periode kan ikke hente opplysninger som bruker periode`() {
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


        verify(exactly = 0) { arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(any(), any(), any()) }
        verify(exactly = 0) { inntektService.hentInntektListe(any(), any(), any()) }
        verify(exactly = 0) { medlPeriodeService.hentPeriodeListe(any(), any(), any()) }

        verify { eregFasade.hentOrganisasjon(any()) }
        verify { behandlingService.lagre(any()) }
    }

    private fun hentBehandling() = Behandling.forTest {
        id = 2L
        tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
    }

    private fun hentBehandling(saksopplysning: Saksopplysning) = hentBehandling().apply {
        saksopplysninger.add(saksopplysning)
    }

    private fun hentSedSaksopplysning(fom: LocalDate, tom: LocalDate) = Saksopplysning().apply {
        dokument = hentSedDokument(fom, tom)
        type = SaksopplysningType.SEDOPPL
    }

    private fun hentSedDokument(fom: LocalDate, tom: LocalDate) = SedDokument().apply {
        lovvalgsperiode = no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom)
        fnr = "123"
    }

    private fun lagSaksopplysning(saksopplysningType: SaksopplysningType) = Saksopplysning().apply {
        type = saksopplysningType
    }

    private fun lagArbeidsforholdDokument() =
        ArbeidsforholdDokument(listOf(Arbeidsforhold().apply {
            arbeidsgiverID = "123456789"
        }))

    private fun registeropplysningerRequest(fom: LocalDate, tom: LocalDate): RegisteropplysningerRequest.RegisteropplysningerRequestBuilder =
        RegisteropplysningerRequest.builder()
            .behandlingID(2L)
            .fom(fom)
            .tom(tom)
            .fnr(FNR)

    private fun saksopplysningstyper(): RegisteropplysningerRequest.SaksopplysningTyper.SaksopplysningTyperBuilder =
        RegisteropplysningerRequest.SaksopplysningTyper.builder()

    private fun hentPeriode(): RegisteropplysningerPeriodeFactory.Periode =
        RegisteropplysningerPeriodeFactory.Periode(YearMonth.now(), YearMonth.now())

    private fun hentDatoPeriode(): RegisteropplysningerPeriodeFactory.DatoPeriode =
        RegisteropplysningerPeriodeFactory.DatoPeriode(LocalDate.now(), LocalDate.now())

    companion object {
        private const val FNR = "432234"
    }
}
