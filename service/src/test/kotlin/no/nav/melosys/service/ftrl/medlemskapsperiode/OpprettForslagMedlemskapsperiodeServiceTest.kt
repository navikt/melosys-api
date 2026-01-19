package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.vilkaarsresultat
import no.nav.melosys.domain.vilkaarsresultatForTest
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelseIkkeYrkesaktiv
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelsePensjonist
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelseYrkesaktiv
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettForslagMedlemskapsperiodeServiceTest {

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var utledMottaksdato: UtledMottaksdato

    @RelaxedMockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    private val vilkårForBestemmelse = VilkårForBestemmelse(
        VilkårForBestemmelseYrkesaktiv(mockk()),
        VilkårForBestemmelseIkkeYrkesaktiv(mockk()),
        VilkårForBestemmelsePensjonist(mockk())
    )

    private lateinit var opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService

    private val BEH_RES_ID: Long = 123321
    private val BESTEMMELSE = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    private val NY_BESTEMMELSE = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD

    @BeforeEach
    fun setup() {
        opprettForslagMedlemskapsperiodeService =
            OpprettForslagMedlemskapsperiodeService(
                behandlingsresultatService,
                utledMottaksdato,
                avklartefaktaService,
                vilkårForBestemmelse
            )
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_lagrerMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat { medAlleVilkårOppfylt() }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE).shouldNotBeEmpty()


        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_medlemskapsperioderEksisterer_oppdatererBestemmelse() {
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medlemskapsperiode {
                id = 1L
                bestemmelse = BESTEMMELSE
            }
            medlemskapsperiode {
                id = 2L
                bestemmelse = BESTEMMELSE
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, NY_BESTEMMELSE)


        perioder.shouldNotBeEmpty()
            .shouldHaveSize(2)
            .run {
                first().run {
                    id.shouldBe(1L)
                    bestemmelse.shouldBe(NY_BESTEMMELSE)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    id.shouldBe(2L)
                    bestemmelse.shouldBe(NY_BESTEMMELSE)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_nyVurdering_kopiererTidligereInnvilgedePerioder_oppdatererBestemmelse() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medFtrlBehandling {
                type = Behandlingstyper.NY_VURDERING
                opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
            }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                tom = LocalDate.now().plusMonths(4)
                bestemmelse = BESTEMMELSE
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            }
            medlemskapsperiode { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, NY_BESTEMMELSE)


        perioder
            .shouldHaveSize(1)
            .first().run {
                tom.shouldBe(LocalDate.now().plusMonths(4))
                bestemmelse.shouldBe(NY_BESTEMMELSE)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_manglendeInnbetalingTrygdeavgift_kopiererTidligereInnvilgedeOgOpphørtePerioder_oppdatererBestemmelsePåInnvilgede() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medFtrlBehandling {
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
            }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat {
            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = LocalDate.now().plusMonths(4)
                bestemmelse = BESTEMMELSE
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            }
            medlemskapsperiode { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT }
            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                fom = LocalDate.now().plusMonths(6)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, NY_BESTEMMELSE)


        perioder.shouldHaveSize(2)
            .sortedBy { it.fom }
            .run {
                first().run {
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    fom.shouldBe(LocalDate.now().plusMonths(4))
                    bestemmelse.shouldBe(NY_BESTEMMELSE)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.OPPHØRT)
                    fom.shouldBe(LocalDate.now().plusMonths(6))
                    bestemmelse.shouldNotBe(NY_BESTEMMELSE)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_andregangsvurderingIngenOpprinneligeMedlemskapsperioder_returnererTomListe() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medFtrlBehandling {
                type = Behandlingstyper.NY_VURDERING
                opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
            }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(behandlingsresultat) } returns behandlingsresultat
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        perioder.shouldNotBeNull().shouldBeEmpty()
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_trygdedekningMedYrkesskadeOgSenSøknadsdato_lagrerAvslåttOgInnvilgetMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medTrygdedekningOgPeriode(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE)
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(2)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.hentTrygdedekning()) {
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                else -> throw TekniskException("Forventet ikke ${medlemskapsperiode.trygdedekning} i denne testen")
            }
        }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_trygdedekningMedYrkesskadeOgMiddelsTidligSøknadsdato_lagrerAvslåttOgInnvilgetMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medTrygdedekningOgPeriode(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE,
                Periode(LocalDate.now().minusYears(1), null)
            )
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(2)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.hentTrygdedekning()) {
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.hentBehandling().mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                else -> throw TekniskException("Forventet ikke ${medlemskapsperiode.trygdedekning} i denne testen")
            }
        }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_trygdedekningMedYrkesskadeOgTidligSøknadsdato_lagrerAvslåttOgInnvilgetMedlemskapsperioder() {
        val søknadsdatoFom = LocalDate.now().minusYears(1)
        val søknadsdatoTom = LocalDate.now().plusMonths(2)
        val mottaksdato = LocalDate.now()
        val behandlingsresultat = lagBehandlingsresultat {
            medAlleVilkårOppfylt()
            medTrygdedekningOgPeriode(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
                Periode(søknadsdatoFom, søknadsdatoTom)
            )
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns mottaksdato


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(4)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.hentTrygdedekning()) {
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(søknadsdatoFom)
                    medlemskapsperiode.tom.shouldBe(søknadsdatoTom)
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(søknadsdatoFom)
                    medlemskapsperiode.tom.shouldBe(mottaksdato.minusDays(1))
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(søknadsdatoFom)
                    medlemskapsperiode.tom.shouldBe(mottaksdato.minusDays(1))
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(mottaksdato)
                    medlemskapsperiode.tom.shouldBe(søknadsdatoTom)
                }

                else -> throw TekniskException("Forventet ikke ${medlemskapsperiode.trygdedekning} i denne testen")
            }
        }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_sakstypeEØS_kasterFeil() {
        val behandlingsresultat = lagBehandlingsresultat()
        behandlingsresultat.behandling!!.fagsak = Fagsak.forTest { type = Sakstyper.EU_EOS }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)
        }.message.shouldContain("Kan ikke opprette medlemskapsperioder for sakstype")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_oppfyllerIkkeVilkår_kasterFeil() {
        val behandlingsresultat = lagBehandlingsresultat()
        behandlingsresultat.behandling!!.tema = Behandlingstema.IKKE_YRKESAKTIV
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { avklartefaktaService.hentAlleAvklarteFakta(any()) } returns emptySet()


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
                BEH_RES_ID,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H
            )
        }.message.shouldContain("er påkrevd for bestemmelse")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_støtterIkkeBestemmelseForDekning_kasterFeil() {
        val ustøttetBestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
        val behandlingsresultat = lagBehandlingsresultat()
        behandlingsresultat.behandling!!.tema = Behandlingstema.IKKE_YRKESAKTIV
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, ustøttetBestemmelse)
        }.message.shouldContain("Ulovlig kombinasjon av bestemmelse")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_harIkkeBestemmelse_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat()


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, null)
        }.message.shouldContain("Bestemmelse er ikke satt")
    }

    /**
     * Helper function to create a base Behandlingsresultat with default FTRL setup.
     * Uses DSL pattern for customization.
     */
    private fun lagBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        behandling {
            id = 543
            fagsak { type = Sakstyper.FTRL }
            tema = Behandlingstema.YRKESAKTIV
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS().apply {
                    periode = Periode(LocalDate.now(), null)
                    soeknadsland.landkoder.add("BR")
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                }
            }
        }
        init()
    }

    /**
     * Extension function to set up behandling with FTRL fagsak and required mottatte opplysninger.
     * Use this when overriding the behandling in tests that need NY_VURDERING or MANGLENDE_INNBETALING.
     */
    private fun BehandlingsresultatTestFactory.Builder.medFtrlBehandling(
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ) {
        behandling {
            id = 543
            fagsak { type = Sakstyper.FTRL }
            tema = Behandlingstema.YRKESAKTIV
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS().apply {
                    periode = Periode(LocalDate.now(), null)
                    soeknadsland.landkoder.add("BR")
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                }
            }
            init()
        }
    }

    /**
     * Extension function to add all required vilkaar for bestemmelse tests.
     */
    private fun BehandlingsresultatTestFactory.Builder.medAlleVilkårOppfylt() {
        vilkaarsresultat {
            vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING
            isOppfylt = true
        }
        vilkaarsresultat {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID
            isOppfylt = true
        }
        vilkaarsresultat {
            vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
            isOppfylt = true
        }
        vilkaarsresultat {
            vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE
            isOppfylt = true
        }
    }

    /**
     * Extension function to override trygdedekning and periode in the soknad.
     * This modifies the already-created behandling's mottatteOpplysninger.
     */
    private fun BehandlingsresultatTestFactory.Builder.medTrygdedekningOgPeriode(
        trygdedekningVerdi: Trygdedekninger,
        periodeVerdi: Periode = Periode(LocalDate.now(), null)
    ) {
        // After behandling {} is called, we need to modify its soknad data
        val soknad = this.behandling?.mottatteOpplysninger?.mottatteOpplysningerData as? SøknadNorgeEllerUtenforEØS
        soknad?.apply {
            trygdedekning = trygdedekningVerdi
            periode = periodeVerdi
        }
    }
}
