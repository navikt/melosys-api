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
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
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

    private val vilkårForBestemmelse = VilkårForBestemmelse(VilkårForBestemmelseYrkesaktiv(mockk()), VilkårForBestemmelseIkkeYrkesaktiv(mockk()), VilkårForBestemmelsePensjonist(mockk()))

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
        val behandlingsresultat = lagBehandlingsresultat().apply { vilkaarsresultater.addAll(lagVilkår()) }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE).shouldNotBeEmpty()


        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_medlemskapsperioderEksisterer_oppdatererBestemmelse() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagVilkår())
            addMedlemskapsperiode(Medlemskapsperiode().apply {
                id = 1L
                bestemmelse = BESTEMMELSE
            })
            addMedlemskapsperiode(Medlemskapsperiode().apply {
                id = 2L
                bestemmelse = BESTEMMELSE
            })
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
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagVilkår())
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            addMedlemskapsperiode(Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                tom = LocalDate.now().plusMonths(4)
                bestemmelse = BESTEMMELSE
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            })
            addMedlemskapsperiode(Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
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
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagVilkår())
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            addMedlemskapsperiode(Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = LocalDate.now().plusMonths(4)
                bestemmelse = BESTEMMELSE
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            })
            addMedlemskapsperiode(Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
            addMedlemskapsperiode(Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                fom = LocalDate.now().plusMonths(6)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            })
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
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagVilkår())
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling.forTest { id = opprinneligBehandlingId }
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
        val behandlingsresultat = lagBehandlingsresultat().apply {
            (behandling.mottatteOpplysninger!!.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS)
                .trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE
            vilkaarsresultater.addAll(lagVilkår())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(2)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.trygdedekning) {
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                else -> throw TekniskException("Forventet ikke ${medlemskapsperiode.trygdedekning} i denne testen")
            }
        }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_trygdedekningMedYrkesskadeOgMiddelsTidligSøknadsdato_lagrerAvslåttOgInnvilgetMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            (behandling.mottatteOpplysninger!!.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS).apply {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE
                periode = Periode(LocalDate.now().minusYears(1), null)
            }
            vilkaarsresultater.addAll(lagVilkår())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(2)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.trygdedekning) {
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
                }

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> {
                    medlemskapsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapsperiode.fom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
                    medlemskapsperiode.tom.shouldBe(behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
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
        val behandlingsresultat = lagBehandlingsresultat().apply {
            (behandling.mottatteOpplysninger!!.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS).apply {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE
                periode = Periode(søknadsdatoFom, søknadsdatoTom)
            }
            vilkaarsresultater.addAll(lagVilkår())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns mottaksdato


        val medlemskapsperioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        medlemskapsperioder.shouldNotBeEmpty()
        medlemskapsperioder.shouldHaveSize(4)
        medlemskapsperioder.forEach { medlemskapsperiode ->
            when (medlemskapsperiode.trygdedekning) {
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
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat().apply {
            behandling.fagsak.type = Sakstyper.EU_EOS
        }


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)
        }.message.shouldContain("Kan ikke opprette medlemskapsperioder for sakstype")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_oppfyllerIkkeVilkår_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat().apply {
            behandling.apply {
                tema = Behandlingstema.IKKE_YRKESAKTIV
            }
        }
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
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns
            lagBehandlingsresultat().apply { behandling.tema = Behandlingstema.IKKE_YRKESAKTIV }


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

    private fun lagBehandlingsresultat(): Behandlingsresultat =
        Behandlingsresultat().apply {
            behandling = Behandling.forTest {
                id = 543
                fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).build()
                tema = Behandlingstema.YRKESAKTIV
                mottatteOpplysninger = MottatteOpplysninger().apply {
                    mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS().apply {
                        periode = Periode(LocalDate.now(), null)
                        soeknadsland.landkoder.add("BR")
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                    }
                }
            }
        }

    private fun lagVilkår(): List<Vilkaarsresultat> =
        listOf(Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING
            isOppfylt = true
        }, Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID
            isOppfylt = true
        }, Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
            isOppfylt = true
        }, Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE
            isOppfylt = true
        })
}
