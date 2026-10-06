package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.persistence.EntityManager
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.repository.*
import no.nav.melosys.service.LovvalgsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LovvalgsperiodeServiceIT(
    @Autowired
    private val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired
    private val tidligereMedlemsperiodeRepository: TidligereMedlemsperiodeRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val entityManager: EntityManager,
    @Autowired
    private val transactionManager: PlatformTransactionManager,
) : DataJpaTestBase() {

    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @BeforeEach
    fun setUp() {
        lovvalgsperiodeService = LovvalgsperiodeService(
            behandlingsresultatRepository,
            lovvalgsperiodeRepository,
            tidligereMedlemsperiodeRepository,
            behandlingRepository
        )
    }

    @Test
    fun `lagreLovvalgsperioder erstatter eksisterende periode med trygdeavgift`() {
        val (behandlingsresultat, _) = lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift()
        val nyLovvalgsperiode = nyLovvalgsperiodeUtenTrygdeavgift().apply {
            setLovvalgsland(Land_iso2.SE)
            setMedlPeriodeID(321L)
        }


        lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiode)
        )


        val lagretPeriode = lovvalgsperiodeRepository
            .findByBehandlingsresultatId(behandlingsresultat.hentId())
            .single()

        lagretPeriode.apply {
            lovvalgsland shouldBe Land_iso2.SE
            medlPeriodeID shouldBe 321L

            trygdeavgiftsperioder.single().apply {
                periodeFra shouldBe TRYGDEAVGIFTSPERIODE_FOM
                periodeTil shouldBe TRYGDEAVGIFTSPERIODE_TOM
                trygdeavgiftsbeløpMd shouldBe Penger(TRYGDEAVGIFTSBELØP)
                trygdesats shouldBe BigDecimal.ONE
                grunnlagInntekstperiode shouldNotBe null
                grunnlagSkatteforholdTilNorge shouldNotBe null
                grunnlagLovvalgsPeriode shouldBe lagretPeriode
                grunnlagMedlemskapsperiode shouldBe null
                grunnlagHelseutgiftDekkesPeriode shouldBe null
            }
        }
    }

    @Test
    fun `lagret lovvalgsperiode er tilgjengelig på behandlingsresultat i samme transaksjon`() {
        val (behandlingsresultat, _) = lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift()

        val nyLovvalgsperiode = nyLovvalgsperiodeUtenTrygdeavgift()
        lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiode)
        )

        // Hent behandlingsresultatet fra persistence context (ikke fra DB)
        // Dette simulerer hva som skjer ved automatisk vedtaksfatting i samme transaksjon
        val hentetBehandlingsresultat = behandlingsresultatRepository.findById(behandlingsresultat.hentId()).get()
        hentetBehandlingsresultat.lovvalgsperioder shouldHaveSize 1
    }

    @Test
    fun `lagreLovvalgsperioder overfører trygdeavgiftsperiode med grunnlagListe til ny lovvalgsperiode`() {
        val behandlingsresultat = lagreBehandlingsresultatMedLovvalgsperiodeSomHarGrunnlagListe()
        entityManager.clear()

        val nyLovvalgsperiode = nyLovvalgsperiodeUtenTrygdeavgift()

        // Uten fix kastet dette TransientPropertyValueException fordi TrygdeavgiftsperiodeGrunnlag
        // refererte en transient (ikke-persistert) Lovvalgsperiode under flush.
        lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiode)
        )

        val lagretPeriode = lovvalgsperiodeRepository
            .findByBehandlingsresultatId(behandlingsresultat.hentId())
            .single()

        lagretPeriode.trygdeavgiftsperioder.single().apply {
            grunnlagListe shouldHaveSize 1
            grunnlagListe.single().apply {
                lovvalgsperiode shouldBe lagretPeriode
                inntektsperiode shouldNotBe null
                skatteforhold shouldNotBe null
            }
            grunnlagLovvalgsPeriode shouldBe lagretPeriode
            grunnlagInntekstperiode shouldNotBe null
            grunnlagSkatteforholdTilNorge shouldNotBe null
        }
    }

    /**
     * melosys-web sender to like lagringer samtidig (MELOSYS-8338). Første kall holder transaksjonen
     * åpen etter at det har skrevet, og andre kall starter i mellomtiden.
     * Uten lås får andre kall ORA-00001 (ingen perioder fra før) eller «delete … row count 0» (perioder fra før).
     */
    @ParameterizedTest(name = "perioder fra før = {0}")
    @ValueSource(booleans = [false, true])
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `to samtidige lagringer for samme behandling lykkes begge`(harPerioderFraFør: Boolean) {
        val behandlingID = iEgenTransaksjon {
            if (harPerioderFraFør) {
                lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift().behandlingsresultat.hentId()
            } else {
                lagreBehandlingsresultatUtenLovvalgsperioder().hentId()
            }
        }
        val førsteHarSkrevet = CountDownLatch(1)
        val andreHarStartet = CountDownLatch(1)
        val førsteKanCommitte = CountDownLatch(1)
        val tråder = Executors.newFixedThreadPool(2)

        try {
            val første = tråder.submit {
                iEgenTransaksjon {
                    lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, listOf(nyLovvalgsperiodeUtenTrygdeavgift()))
                    lovvalgsperiodeRepository.flush()
                    førsteHarSkrevet.countDown()
                    førsteKanCommitte.await(10, TimeUnit.SECONDS)
                }
            }
            førsteHarSkrevet.await(10, TimeUnit.SECONDS) shouldBe true
            val andre = tråder.submit {
                andreHarStartet.countDown()
                iEgenTransaksjon {
                    lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, listOf(nyLovvalgsperiodeUtenTrygdeavgift()))
                }
            }

            andreHarStartet.await(10, TimeUnit.SECONDS) shouldBe true
            Thread.sleep(500)
            withClue("andre kall skal vente på første kall") { andre.isDone shouldBe false }
            førsteKanCommitte.countDown()

            første.get(30, TimeUnit.SECONDS)
            andre.get(30, TimeUnit.SECONDS)
        } finally {
            førsteKanCommitte.countDown()
            tråder.shutdownNow()
            tråder.awaitTermination(15, TimeUnit.SECONDS)
        }

        lovvalgsperiodeRepository.findByBehandlingsresultatId(behandlingID).single().apply {
            fom shouldBe NY_LOVVALGSPERIODE_FOM
            tom shouldBe NY_LOVVALGSPERIODE_TOM
        }
    }

    private fun <T> iEgenTransaksjon(blokk: () -> T): T = TransactionTemplate(transactionManager).execute { blokk() }!!

    private fun lagreBehandlingsresultatUtenLovvalgsperioder(): Behandlingsresultat =
        behandlingsresultatRepository.saveAndFlush(
            Behandlingsresultat.forTest {
                behandling = lagreBehandling()
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            }.also { it.leggTilRegisteringInfo() }
        )

    private fun lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift(
        behandlingstema: Behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    ): LagretLovvalgsperiodeMedResultat {
        val lagretBehandling = lagreBehandling(behandlingstema)

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = lagretBehandling
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }.also {
            it.leggTilRegisteringInfo()
        }

        val lagretBehandlingsresultat = behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
        val lovvalgsperiode = lagLovvalgsperiodeMedTrygdeavgiftsperiode(lagretBehandlingsresultat)
        lagretBehandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)

        return LagretLovvalgsperiodeMedResultat(
            behandlingsresultat = lagretBehandlingsresultat,
            lovvalgsperiode = lovvalgsperiode
        )
    }

    private fun lagreBehandling(
        behandlingstema: Behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    ): Behandling {
        val fagsak = Fagsak.forTest {
            saksnummer = "MEL-${UUID.randomUUID()}"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
        }.also {
            it.leggTilRegisteringInfo()
        }

        val lagretFagsak = fagsakRepository.save(fagsak)

        val behandling = Behandling.forTest {
            id = 0
            this.fagsak = lagretFagsak
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = behandlingstema
            behandlingsfrist = BEHANDLINGSFRIST
        }.also {
            it.leggTilRegisteringInfo()
        }

        return behandlingRepository.save(behandling)
    }

    private fun lagLovvalgsperiodeMedTrygdeavgiftsperiode(
        lagretBehandlingsresultat: Behandlingsresultat
    ): Lovvalgsperiode {
        return Lovvalgsperiode.forTest {
            behandlingsresultat = lagretBehandlingsresultat
            fom = EKSISTERENDE_LOVVALGSPERIODE_FOM
            tom = EKSISTERENDE_LOVVALGSPERIODE_TOM
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING
            medlPeriodeID = 123L
        }.apply {
            addTrygdeavgiftsperiode(
                Trygdeavgiftsperiode.forTest {
                    periodeFra = TRYGDEAVGIFTSPERIODE_FOM
                    periodeTil = TRYGDEAVGIFTSPERIODE_TOM
                    trygdeavgiftsbeløpMd = TRYGDEAVGIFTSBELØP
                    trygdesats = BigDecimal.ONE
                }
            )
        }
    }

    private fun nyLovvalgsperiodeUtenTrygdeavgift() = Lovvalgsperiode.forTest {
        fom = NY_LOVVALGSPERIODE_FOM
        tom = NY_LOVVALGSPERIODE_TOM
        lovvalgsland = Land_iso2.NO
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        dekning = Trygdedekninger.FULL_DEKNING
        medlPeriodeID = 999L
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        registrertAv = TEST_BRUKER
        endretAv = TEST_BRUKER
    }

    private data class LagretLovvalgsperiodeMedResultat(
        val behandlingsresultat: Behandlingsresultat,
        val lovvalgsperiode: Lovvalgsperiode
    )

    private fun lagreBehandlingsresultatMedLovvalgsperiodeSomHarGrunnlagListe(): Behandlingsresultat {
        val lagretBehandling = lagreBehandling()

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = lagretBehandling
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }.also { it.leggTilRegisteringInfo() }

        val lagretBr = behandlingsresultatRepository.saveAndFlush(behandlingsresultat)

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = TRYGDEAVGIFTSPERIODE_FOM
            tomDato = TRYGDEAVGIFTSPERIODE_TOM
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(BigDecimal("15000"))
            isArbeidsgiversavgiftBetalesTilSkatt = false
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = TRYGDEAVGIFTSPERIODE_FOM
            tomDato = TRYGDEAVGIFTSPERIODE_TOM
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        val lovvalgsperiode = Lovvalgsperiode.forTest {
            this.behandlingsresultat = lagretBr
            fom = EKSISTERENDE_LOVVALGSPERIODE_FOM
            tom = EKSISTERENDE_LOVVALGSPERIODE_TOM
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING
            medlPeriodeID = 123L
        }

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = TRYGDEAVGIFTSPERIODE_FOM,
            periodeTil = TRYGDEAVGIFTSPERIODE_TOM,
            trygdeavgiftsbeløpMd = Penger(TRYGDEAVGIFTSBELØP),
            trygdesats = BigDecimal.ONE,
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold,
        )

        trygdeavgiftsperiode.leggTilGrunnlag(
            TrygdeavgiftsperiodeGrunnlag(
                trygdeavgiftsperiode = trygdeavgiftsperiode,
                lovvalgsperiode = lovvalgsperiode,
                inntektsperiode = inntektsperiode,
                skatteforhold = skatteforhold,
            )
        )

        lovvalgsperiode.addTrygdeavgiftsperiode(trygdeavgiftsperiode)
        lagretBr.lovvalgsperioder.add(lovvalgsperiode)
        behandlingsresultatRepository.saveAndFlush(lagretBr)

        return lagretBr
    }

    companion object {
        private val NY_LOVVALGSPERIODE_FOM = LocalDate.of(2024, 1, 1)
        private val NY_LOVVALGSPERIODE_TOM = LocalDate.of(2024, 6, 30)
        private val TRYGDEAVGIFTSPERIODE_FOM = LocalDate.of(2024, 1, 1)
        private val TRYGDEAVGIFTSPERIODE_TOM = LocalDate.of(2024, 1, 31)
        private val EKSISTERENDE_LOVVALGSPERIODE_FOM = LocalDate.of(2023, 7, 1)
        private val EKSISTERENDE_LOVVALGSPERIODE_TOM = LocalDate.of(2023, 10, 1)
        private val BEHANDLINGSFRIST = LocalDate.of(2024, 12, 31)
        private val TRYGDEAVGIFTSBELØP = BigDecimal.valueOf(1000)
        private const val TEST_BRUKER = "test"
    }
}
