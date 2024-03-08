package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.getunleash.FakeUnleash
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
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettForslagMedlemskapsperiodeServiceTest {

    @MockK
    private lateinit var medlemAvFolketrygdenRepository: MedlemAvFolketrygdenRepository

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var utledMottaksdato: UtledMottaksdato

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    private val fakeUnleash = FakeUnleash()

    private val utledBestemmelserOgVilkår = UtledBestemmelserOgVilkår(fakeUnleash)

    private val vilkårForBestemmelse = VilkårForBestemmelse(VilkårForBestemmelseYrkesaktiv(mockk()), VilkårForBestemmelseIkkeYrkesaktiv(mockk()), fakeUnleash)

    private lateinit var opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService

    private val BEH_RES_ID: Long = 123321
    private val BESTEMMELSE = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    private val NY_BESTEMMELSE = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD

    @BeforeEach
    fun setup() {
        fakeUnleash.resetAll()
        opprettForslagMedlemskapsperiodeService =
            OpprettForslagMedlemskapsperiodeService(
                medlemAvFolketrygdenRepository,
                behandlingsresultatService,
                utledMottaksdato,
                utledBestemmelserOgVilkår,
                fakeUnleash,
                avklartefaktaService,
                vilkårForBestemmelse
            )
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_lagrerMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat().apply { vilkaarsresultater.add(lagVilkår()) }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { medlemAvFolketrygdenRepository.save(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE).shouldNotBeEmpty()


        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_medlemskapsperioderEksisterer_oppdatererBestemmelse() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagAlleKrevdeVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                addMedlemskapsperiode(Medlemskapsperiode().apply {
                    id = 1L
                    bestemmelse = BESTEMMELSE
                })
                addMedlemskapsperiode(Medlemskapsperiode().apply {
                    id = 2L
                    bestemmelse = BESTEMMELSE
                })
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


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
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_nyVurdering_kopiererTidligereInnvilgedePerioder_oppdatererBestemmelse() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagAlleKrevdeVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden()
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling().apply { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                addMedlemskapsperiode(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    tom = LocalDate.now().plusMonths(4)
                    bestemmelse = BESTEMMELSE
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                })
                addMedlemskapsperiode(Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, NY_BESTEMMELSE)


        perioder
            .shouldHaveSize(1)
            .first().run {
                tom.shouldBe(LocalDate.now().plusMonths(4))
                bestemmelse.shouldBe(NY_BESTEMMELSE)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_manglendeInnbetalingTrygdeavgift_kopiererTidligereInnvilgedeOgOpphørtePerioder_oppdatererBestemmelsePåInnvilgede() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagAlleKrevdeVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden()
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = Behandling().apply { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
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
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


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
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_andregangsvurderingIngenOpprinneligeMedlemskapsperioder_returnererTomListe() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.addAll(lagAlleKrevdeVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden()
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling().apply { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply { medlemAvFolketrygden = MedlemAvFolketrygden() }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        val perioder = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)


        perioder.shouldNotBeNull().shouldBeEmpty()
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
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

    @Deprecated("MELOSYS_FTRL_IKKE_YRKESAKTIV melosys.ftrl.ikke_yrkesaktiv")
    @Test
    fun opprettForslagPåMedlemskapsperioder_oppfyllerIkkeVilkår_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat().apply {
            vilkaarsresultater.add(lagVilkår(false))
        }


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, BESTEMMELSE)
        }.message.shouldContain("er påkrevd for bestemmelse")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_toggle_oppfyllerIkkeVilkår_kasterFeil() {
        fakeUnleash.enable(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV)
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat().apply {
            behandling.apply {
                tema = Behandlingstema.IKKE_YRKESAKTIV
            }
            vilkaarsresultater.add(lagVilkår(false))
        }
        every { avklartefaktaService.hentAlleAvklarteFakta(any()) } returns emptySet()


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
                BEH_RES_ID,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H
            )
        }.message.shouldContain("er påkrevd for bestemmelse")
    }

    @Deprecated("MELOSYS_FTRL_IKKE_YRKESAKTIV melosys.ftrl.ikke_yrkesaktiv")
    @Test
    fun opprettForslagPåMedlemskapsperioder_støtterIkkeBestemmelse_kasterFeil() {
        val ustøttetBestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_RES_ID) } returns lagBehandlingsresultat()


        shouldThrow<FunksjonellException> {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(BEH_RES_ID, ustøttetBestemmelse)
        }.message.shouldContain("Støtter ikke")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_støtterIkkeBestemmelseForDekning_kasterFeil() {
        fakeUnleash.enable(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV)
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
            medlemAvFolketrygden = MedlemAvFolketrygden()
            behandling = Behandling().apply {
                id = 543
                fagsak = Fagsak().apply { type = Sakstyper.FTRL }
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

    private fun lagVilkår(oppfylt: Boolean = true): Vilkaarsresultat =
        Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID
            isOppfylt = oppfylt
        }

    private fun lagAlleKrevdeVilkår(): List<Vilkaarsresultat> =
        listOf(Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID
            isOppfylt = true
        }, Vilkaarsresultat().apply {
            vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
            isOppfylt = true
        })
}
