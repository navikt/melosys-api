package no.nav.melosys.service.medlemskapsperiode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettMedlemskapsperiodeServiceTest {
    @MockK
    private lateinit var medlemAvFolketrygdenRepository: MedlemAvFolketrygdenRepository

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var utledMottaksdato: UtledMottaksdato

    private lateinit var opprettMedlemskapsperiodeService: OpprettMedlemskapsperiodeService

    private val behandlingsresultatID: Long = 123321

    @BeforeEach
    fun setup() {
        opprettMedlemskapsperiodeService =
            OpprettMedlemskapsperiodeService(medlemAvFolketrygdenRepository, behandlingsresultatService, utledMottaksdato)
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_lagrerMedlemskapsperioder() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.add(lagOppfyltVilkår())
            medlemAvFolketrygden.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns behandlingsresultat
        every { medlemAvFolketrygdenRepository.save(any()) } returnsArgument 0
        every { utledMottaksdato.getMottaksdato(any()) } returns LocalDate.now()


        opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID).shouldNotBeEmpty()


        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_dataFraSøknadSatt_medlemskapsperioderEksisterer() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.add(lagOppfyltVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                addMedlemskapsperiode(Medlemskapsperiode())
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns behandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


        opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID).shouldNotBeEmpty()


        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }

    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_nyVurdering_kopiererTidligereInnvilgedePerioder() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.add(lagOppfyltVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
            }
            behandling.type = Behandlingstyper.NY_VURDERING
            behandling.opprinneligBehandling = Behandling().apply { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                addMedlemskapsperiode(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    tom = LocalDate.now().plusMonths(4)
                })
                addMedlemskapsperiode(Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


        opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
            .shouldHaveSize(1)
            .first().run {
                tom.shouldBe(LocalDate.now().plusMonths(4))
            }
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_manglendeInnbetalingTrygdeavgift_kopiererTidligereInnvilgedePerioder() {
        val opprinneligBehandlingId = 2L
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater.add(lagOppfyltVilkår())
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
            }
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = Behandling().apply { id = opprinneligBehandlingId }
        }
        val opprinneligBehandlingsresultat = lagBehandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                addMedlemskapsperiode(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    tom = LocalDate.now().plusMonths(4)
                })
                addMedlemskapsperiode(Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns opprinneligBehandlingsresultat
        every { medlemAvFolketrygdenRepository.save(behandlingsresultat.medlemAvFolketrygden) } returns behandlingsresultat.medlemAvFolketrygden


        opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
            .shouldHaveSize(1)
            .first().run {
                tom.shouldBe(LocalDate.now().plusMonths(4))
            }
        verify(exactly = 1) { medlemAvFolketrygdenRepository.save(any()) }
        verify(exactly = 0) { utledMottaksdato.getMottaksdato(any()) }
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_sakstypeEØS_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns lagBehandlingsresultat().apply {
            behandling.fagsak.type = Sakstyper.EU_EOS
        }


        shouldThrow<FunksjonellException> {
            opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
        }.message.shouldContain("Kan ikke opprette medlemskapsperioder for sakstype")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_oppfyllerIkkeVilkår_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns lagBehandlingsresultat().apply {
            medlemAvFolketrygden.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
            vilkaarsresultater.add(lagOppfyltVilkår().apply { isOppfylt = false })
        }


        shouldThrow<FunksjonellException> {
            opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
        }.message.shouldContain("er påkrevd for bestemmelse")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_støtterIkkeBestemmelse_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns lagBehandlingsresultat().apply {
            medlemAvFolketrygden.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D
        }


        shouldThrow<FunksjonellException> {
            opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
        }.message.shouldContain("Støtter ikke")
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder_harIkkeBestemmelse_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID) } returns lagBehandlingsresultat()


        shouldThrow<FunksjonellException> {
            opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingsresultatID)
        }.message.shouldContain("Bestemmelse er ikke satt")
    }

    private fun lagBehandlingsresultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        val behandling = Behandling()
        val fagsak = Fagsak()
        fagsak.type = Sakstyper.FTRL
        behandling.fagsak = fagsak
        behandling.tema = Behandlingstema.YRKESAKTIV
        val mottatteOpplysninger = MottatteOpplysninger()
        val søknad = SøknadNorgeEllerUtenforEØS()
        søknad.periode = Periode(LocalDate.now(), null)
        søknad.soeknadsland.landkoder.add("BR")
        søknad.trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
        mottatteOpplysninger.setMottatteOpplysningerdata(søknad)
        behandling.mottatteOpplysninger = mottatteOpplysninger
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden()
        behandlingsresultat.behandling = behandling
        return behandlingsresultat
    }

    private fun lagOppfyltVilkår(): Vilkaarsresultat {
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.vilkaar = Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID
        vilkaarsresultat.isOppfylt = true
        return vilkaarsresultat
    }
}
