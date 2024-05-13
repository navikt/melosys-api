package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.GyldigeTrygdedekningerService
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class MedlemskapsperiodeServiceTest {

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var medlemAvFolketrygdenService: MedlemAvFolketrygdenService

    @RelaxedMockK
    lateinit var trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService

    @MockK
    lateinit var medlemskapsperiodeRepository: MedlemskapsperiodeRepository

    @RelaxedMockK
    lateinit var medlPeriodeService: MedlPeriodeService

    @MockK
    lateinit var gyldigeTrygdedekningerService: GyldigeTrygdedekningerService

    private val unleash = FakeUnleash()

    private val BEHANDLING_ID_1 = 1L
    private val BEHANDLING_ID_2 = 2L
    private val MEDLEMSKAPSPERIODE_ID_1 = 11L
    private val MEDLEMSKAPSPERIODE_ID_2 = 22L
    private val MEDL_ID_1 = 111L
    private val MEDL_ID_2 = 222L
    private val NÅ = LocalDate.now()

    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        medlemskapsperiodeService = MedlemskapsperiodeService(
            medlemskapsperiodeRepository,
            medlemAvFolketrygdenService,
            trygdeavgiftsgrunnlagService,
            medlPeriodeService,
            gyldigeTrygdedekningerService,
            unleash
        )
    }

    @Test
    fun hentMedlemskapsperioder_finnesMedlemAvFolketrygden_returnererMedlemskapsperioder() {
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(
            MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode(), Medlemskapsperiode())
            })


        medlemskapsperiodeService.hentMedlemskapsperioder(BEHANDLING_ID_1).shouldHaveSize(2)
    }

    @Test
    fun hentMedlemskapsperioder_ingenMedlemAvFolketrygden_returnererTomListe() {
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.ofNullable(null)


        medlemskapsperiodeService.hentMedlemskapsperioder(BEHANDLING_ID_1).shouldNotBeNull().shouldBeEmpty()
    }

    @Test
    fun opprettMedlemskapsperiode_lagrerKorrektMedlemskapsperiode() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { behandlingsresultat = lagBehandlingsresultat() }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden
        every { medlemskapsperiodeRepository.save(any()) } returnsArgument 0


        medlemskapsperiodeService.opprettMedlemskapsperiode(
            BEHANDLING_ID_1,
            NÅ,
            NÅ,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
        )


        verify { medlemskapsperiodeRepository.save(any()) }
        verify(exactly = 0) { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(any()) }
        medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(1).single().run {
            fom.shouldBe(NÅ)
            tom.shouldBe(NÅ)
            innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
            bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD)
            medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
        }
    }


    @Test
    fun opprettMedlemskapsperiode_harFastsattTrygdeavgift_fjernerTrygdeavgiftsperioderOmDeFinnes() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
            fastsattTrygdeavgift = FastsattTrygdeavgift()
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden
        every { medlemskapsperiodeRepository.save(any()) } returnsArgument 0


        medlemskapsperiodeService.opprettMedlemskapsperiode(
            BEHANDLING_ID_1,
            NÅ,
            NÅ,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
        )


        verify { medlemskapsperiodeRepository.save(any()) }
        verify { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift) }
        medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(1)
    }

    @Test
    fun oppdaterMedlemskapsperiode_medlemskapsperiodeFinnes_oppdateres() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
            behandlingsresultat = lagBehandlingsresultat()
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden
        every { medlemskapsperiodeRepository.saveAndFlush(any()) } returnsArgument 0


        medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            BEHANDLING_ID_1,
            MEDLEMSKAPSPERIODE_ID_1,
            NÅ,
            NÅ,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
        )


        verify { medlemskapsperiodeRepository.saveAndFlush(any()) }
        verify(exactly = 0) { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(any()) }
        medlemAvFolketrygden.medlemskapsperioder.single().run {
            fom.shouldBe(NÅ)
            tom.shouldBe(NÅ)
            innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER)
            bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1)
        }
    }

    @Test
    fun oppdaterMedlemskapsperiode_medlemskapsperiodeOgFastsattTrygdeavgiftFinnes_oppdateres() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
            fastsattTrygdeavgift = FastsattTrygdeavgift()
            behandlingsresultat = lagBehandlingsresultat()
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden
        every { medlemskapsperiodeRepository.saveAndFlush(any()) } returnsArgument 0


        medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            BEHANDLING_ID_1,
            MEDLEMSKAPSPERIODE_ID_1,
            NÅ,
            NÅ,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
        )


        verify { medlemskapsperiodeRepository.saveAndFlush(any()) }
        verify { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift) }
    }

    @Test
    fun `opprettMedlemskapsperiode kaster ikke exception når tomDato er null, land er Norge og bestemmelse er 2_1`() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        unleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { behandlingsresultat = lagBehandlingsresultat(Land_iso2.NO) }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden
        every { medlemskapsperiodeRepository.save(any()) } returnsArgument 0


        medlemskapsperiodeService.opprettMedlemskapsperiode(
            BEHANDLING_ID_1,
            NÅ,
            null,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
        )


        verify { medlemskapsperiodeRepository.save(any()) }
    }

    @Test
    fun `opprettMedlemskapsperiode kaster exception når tomDato er null og bestemmelse ikke er 2_1`() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat(Land_iso2.AU).apply { behandling.tema = Behandlingstema.IKKE_YRKESAKTIV }
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden


        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.opprettMedlemskapsperiode(
                BEHANDLING_ID_1,
                NÅ,
                null,
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                PliktigeMedlemskapsbestemmelser.bestemmelser[0]
            )
        }.message.shouldContain("Tom-dato er påkrevd")
    }

    @Test
    fun oppdaterMedlemskapsperiode_ugyldigTrygdedekning_kasterException() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns emptyList()
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
        }


        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                MEDLEMSKAPSPERIODE_ID_1,
                NÅ,
                NÅ,
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.UTEN_DEKNING,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
        }.message.shouldContain("støttes ikke")
    }

    @Test
    fun oppdaterMedlemskapsperiode_opphoertBestemmelse_kasterIkkeException() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(Behandlingstema.YRKESAKTIV, null) } returns
            listOf(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
            medlemskapsperioder = listOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
        }
        every { medlemskapsperiodeRepository.saveAndFlush(any()) } returnsArgument 0


        shouldNotThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                MEDLEMSKAPSPERIODE_ID_1,
                NÅ,
                NÅ,
                InnvilgelsesResultat.OPPHØRT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
            )
        }
    }

    @Test
    fun oppdaterMedlemskapsperiode_tomDatoErFørFomDato_kasterException() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
        }


        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                MEDLEMSKAPSPERIODE_ID_1,
                NÅ,
                NÅ.minusDays(1),
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
        }.message.shouldBe("Tom-dato kan ikke være før fom-dato")
    }

    @Test
    fun oppdaterMedlemskapsperiode_utenTrygdedekning_kasterFeil() {
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
        }


        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                MEDLEMSKAPSPERIODE_ID_1,
                NÅ,
                NÅ,
                InnvilgelsesResultat.AVSLAATT,
                null,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
        }.message.shouldContain("Fom-dato, innvilgelsesresultat, bestemmelse og trygdedekning er påkrevd")
    }

    @Test
    fun oppdaterMedlemskapsperiode_utenBestemmelse_kasterFeil() {
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            behandlingsresultat = lagBehandlingsresultat()
        }

        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                MEDLEMSKAPSPERIODE_ID_1,
                NÅ,
                NÅ,
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                null
            )
        }.message.shouldContain("Fom-dato, innvilgelsesresultat, bestemmelse og trygdedekning er påkrevd")
    }

    @Test
    fun oppdaterMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        every { gyldigeTrygdedekningerService.hentTrygdedekninger(any(), any()) } returns listOf(*Trygdedekninger.values())
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
            behandlingsresultat = lagBehandlingsresultat()
        }


        shouldThrow<FunksjonellException> {
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                BEHANDLING_ID_1,
                0,
                NÅ,
                NÅ,
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
            )
        }.message.shouldContain("Behandling 1 har ingen medlemskapsperiode med id 0")
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun opprette nye perioder når gammel liste er tom`() {
        every { medlemskapsperiodeRepository.save(any()) } returnsArgument 0
        val medlemskapsperiode1 = Medlemskapsperiode().apply {
            fom = NÅ.minusDays(1)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val medlemskapsperiode2 = Medlemskapsperiode().apply {
            fom = NÅ.plusDays(1)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val nyListe = listOf(medlemskapsperiode1, medlemskapsperiode2)
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.empty()


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, nyListe)


        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(BEHANDLING_ID_2, medlemskapsperiode1) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(BEHANDLING_ID_2, medlemskapsperiode2) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun avvise gamle perioder når ny liste er tom`() {
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelListe = listOf(gammelMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }

        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, emptyList())


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_1) }
        verify(exactly = 0) { medlPeriodeService.opprettPeriodeEndelig(any<Long>(), any()) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal opprette nye og avvise gamle når begge lister ikke er tomme og det er ingen felles elementer`() {
        every { medlemskapsperiodeRepository.save(any()) } returnsArgument 0
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelMedlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(gammelMedlemskapsperiode) }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(gammelMedlemAvFolketrygden)
        val nyMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, listOf(nyMedlemskapsperiode))


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_1) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(BEHANDLING_ID_2, nyMedlemskapsperiode) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal oppdatere periode når begge lister har felles elementer`() {
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val fellesMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_2
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelListe = listOf(fellesMedlemskapsperiode, gammelMedlemskapsperiode)
        val nyMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }
        val nyListe = listOf(fellesMedlemskapsperiode, nyMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, nyListe)


        verify(exactly = 1) { medlPeriodeService.oppdaterPeriodeEndelig(BEHANDLING_ID_2, fellesMedlemskapsperiode) }
        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_2) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(BEHANDLING_ID_2, nyMedlemskapsperiode) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal fjerne innvilgede perioder som blir avslått`() {
        val gammelPeriode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val nyPeriode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns
            Optional.of(MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(gammelPeriode) })


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, listOf(nyPeriode))


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_1) }
        confirmVerified(medlPeriodeService)
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun avvise gamle perioder som er innvilget`() {
        val gammelMedlemskapsperiodeInnvilget = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelMedlemskapsperiodeAvslag = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_2
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
        val gammelListe = listOf(gammelMedlemskapsperiodeInnvilget, gammelMedlemskapsperiodeAvslag)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, emptyList())


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_1) }
        verify(exactly = 0) { medlPeriodeService.avvisPeriodeOpphørt(MEDL_ID_2) }
        verify(exactly = 0) { medlPeriodeService.opprettPeriodeEndelig(any<Long>(), any()) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal kunne opprette nye opphørte perioder`() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = emptyList() }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)
        val nyInnvilgetMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }
        val nyOpphørtMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT }


        medlemskapsperiodeService.erstattMedlemskapsperioder(
            BEHANDLING_ID_2,
            BEHANDLING_ID_1,
            listOf(nyInnvilgetMedlemskapsperiode, nyOpphørtMedlemskapsperiode)
        )


        verify(exactly = 1) { medlPeriodeService.opprettOpphørtPeriode(BEHANDLING_ID_2, nyOpphørtMedlemskapsperiode) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal kunne oppdatere opphørte perioder som videreføres fra tidligere behandling`() {
        val videreførtMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(videreførtMedlemskapsperiode) }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)
        val videreførtMedlemskapsperiodeOpphøres = videreførtMedlemskapsperiode.apply {
            innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
        }


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, listOf(videreførtMedlemskapsperiodeOpphøres))


        verify(exactly = 1) { medlPeriodeService.oppdaterOpphørtPeriode(BEHANDLING_ID_2, videreførtMedlemskapsperiodeOpphøres) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal feilregistrere opphørte perioder som ikke videreføres fra tidligere behandling`() {
        val gammelOpphørtPeriode = Medlemskapsperiode().apply {
            medlPeriodeID = MEDL_ID_1
            innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
        }
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(gammelOpphørtPeriode) }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID_1) } returns Optional.of(medlemAvFolketrygden)
        val nyMedlemskapsperiode = Medlemskapsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }


        medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID_2, BEHANDLING_ID_1, listOf(nyMedlemskapsperiode))


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeFeilregistrert(gammelOpphørtPeriode.medlPeriodeID) }
    }

    @Test
    fun `opprettEllerOppdaterMedlPeriode oppretter når medlId ikke finnes`() {
        val medlemskapsperiodeUtenMedlId = Medlemskapsperiode()


        medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(BEHANDLING_ID_1, medlemskapsperiodeUtenMedlId)


        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(BEHANDLING_ID_1, medlemskapsperiodeUtenMedlId) }
    }

    @Test
    fun `opprettEllerOppdaterMedlPeriode oppdaterer når medlId finnes`() {
        val medlemskapsperiodeMedMedlId = Medlemskapsperiode().apply { medlPeriodeID = 1L }


        medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(BEHANDLING_ID_1, medlemskapsperiodeMedMedlId)


        verify(exactly = 1) { medlPeriodeService.oppdaterPeriodeEndelig(BEHANDLING_ID_1, medlemskapsperiodeMedMedlId) }
    }

    @Test
    fun slettMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns MedlemAvFolketrygden().apply {
            medlemskapsperioder = mutableListOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
        }


        shouldThrow<FunksjonellException> { medlemskapsperiodeService.slettMedlemskapsperiode(BEHANDLING_ID_1, MEDLEMSKAPSPERIODE_ID_2) }
            .message.shouldBe("Finner ingen medlemskapsperiode med id 22 for behandling 1")
    }

    @Test
    fun slettMedlemskapsperiode_finnesToMedlemskapsperioder_sletterDenEne() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = mutableListOf(
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 },
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_2 })
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden


        medlemskapsperiodeService.slettMedlemskapsperiode(BEHANDLING_ID_1, MEDLEMSKAPSPERIODE_ID_1)


        verify(exactly = 0) { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(any()) }
        medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(1)
    }

    @Test
    fun slettMedlemskapsperiode_fjernTrygdeavgiftsperioder_slettes() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = mutableListOf(Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 })
            fastsattTrygdeavgift = FastsattTrygdeavgift()
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden


        medlemskapsperiodeService.slettMedlemskapsperiode(BEHANDLING_ID_1, MEDLEMSKAPSPERIODE_ID_1)


        verify { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift) }
        medlemAvFolketrygden.medlemskapsperioder.shouldBeEmpty()
    }

    @Test
    fun slettMedlemskapsperioder_finnesToMedlemskapsperioder_sletterAlle() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = mutableListOf(
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 },
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_2 })
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden


        medlemskapsperiodeService.slettMedlemskapsperioder(BEHANDLING_ID_1)


        verify(exactly = 0) { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(any()) }
        medlemAvFolketrygden.medlemskapsperioder.shouldBeEmpty()
    }

    @Test
    fun slettMedlemskapsperioder_finnesFastsattTrygdeavgift_fjernerTrygdeavgiftsperioderOmDeFinnes() {
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = mutableListOf(
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_1 },
                Medlemskapsperiode().apply { id = MEDLEMSKAPSPERIODE_ID_2 })
            fastsattTrygdeavgift = FastsattTrygdeavgift()
        }
        every { medlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID_1) } returns medlemAvFolketrygden


        medlemskapsperiodeService.slettMedlemskapsperioder(BEHANDLING_ID_1)


        verify { trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift) }
        medlemAvFolketrygden.medlemskapsperioder.shouldBeEmpty()
    }

    private fun lagBehandlingsresultat(land: Land_iso2 = Land_iso2.AU): Behandlingsresultat = Behandlingsresultat().apply {
        behandling = Behandling().apply {
            id = BEHANDLING_ID_1
            tema = Behandlingstema.YRKESAKTIV
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS().apply {
                    soeknadsland = Soeknadsland(listOf(land.kode), false)
                }
            }
        }
    }
}
