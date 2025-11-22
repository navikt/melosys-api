package no.nav.melosys.itest.vedtak

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.melosysmock.config.SoapConfig
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.sak.SøknadDto
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

/**
 * Integration test that verifies the fix for the race condition in vedtak creation.
 *
 * Previously, EosVedtakService would trigger optimistic locking failures (StaleObjectStateException)
 * when creating vedtak because:
 * 1. registeropplysninger were fetched and saved (updating SaksopplysningKilde entities)
 * 2. Behandling entity was reloaded by ID
 * 3. Hibernate tried to synchronize the stale entities → optimistic lock failure
 *
 * The fix passes the Behandling object through the validation chain instead of reloading it,
 * preventing the race condition.
 *
 * This test creates multiple vedtak to verify the fix works consistently.
 */
@Import(SoapConfig::class)
class EosVedtakRaceConditionIT(
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
    @Autowired private val ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val utpekingService: UtpekingService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val opprettSak: OpprettSak
) : JournalfoeringBase() {

    @MockkBean
    private lateinit var utstedtA1AivenProducer: UtstedtA1AivenProducer
    private var originalSubjectHandler: SubjectHandler? = null

    @BeforeEach
    fun setup() {
        originalSubjectHandler = SubjectHandler.getInstance()
        val mockHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(mockHandler)
        every { mockHandler.userID } returns "Z123456"
        every { mockHandler.userName } returns "test"

        MedlRepo.repo.clear()
    }

    @AfterEach
    fun afterEach() {
        MedlRepo.repo.clear()
        SubjectHandler.set(originalSubjectHandler)
    }

    @Test
    fun `skal ikke få optimistic locking feil ved vedtak creation - kjør 5 ganger for å verifisere stabilitet`() {
        // Run vedtak creation 5 times to verify no race condition occurs
        // Previously this would fail 2/3 times with StaleObjectStateException
        repeat(5) { iteration ->
            // Given - Create new behandling for each iteration
            val behandling = journalførOgVentTilProsesserErFerdige(
                defaultJournalføringDto().apply {
                    fagsak.sakstype = Sakstyper.EU_EOS.kode
                    fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                    behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                    behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                },
                mapOf<ProsessType, Int>(
                    ProsessType.JFR_NY_SAK_BRUKER to 1,
                    ProsessType.JFR_JOURNALPOST to 1
                )
            ).behandling.shouldNotBeNull()

            // Setup behandling data
            mottatteOpplysningerService.oppfriskMottatteOpplysninger(
                behandling.id,
                defaultSøknadDto()
            )

            avklartefaktaService.lagreAvklartefakta(
                behandling.id,
                defaultAvklartefaktaDto()
            )

            vilkaarsresultatService.lagreVilkaar(
                behandling.id,
                defaultVilkaarDto()
            )

            // Create lovvalgsperiode
            opprettSak.opprettLovvalgsperiode(
                behandling.id,
                OpprettSakDto(
                    land = Land_iso2.DK,
                    behandlingsmaate = Behandlingsmaate.AUTOMATISK,
                    periode = PeriodeDto(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusYears(2)
                    )
                )
            )

            // Refresh registeropplysninger (simulates the condition that triggers race)
            oppfriskSaksopplysningerService.oppfriskOgLagreRegisteropplysninger(behandling.id)

            // When - Fatt vedtak (this is where the race condition previously occurred)
            val behandlingEtterVedtak = try {
                vedtaksfattingFasade.fattVedtakEos(
                    behandling.id,
                    FattVedtakRequest.Builder()
                        .medBehandlingsresultatType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
                        .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
                        .medMottakerInstitusjoner(emptySet())
                        .medKopiTilArbeidsgiver(true)
                        .build()
                )
                behandlingRepository.findById(behandling.id).orElseThrow()
            } catch (e: org.springframework.orm.ObjectOptimisticLockingFailureException) {
                throw AssertionError(
                    "Iteration ${iteration + 1}/5 failed with optimistic locking exception. " +
                    "This indicates the race condition fix is not working. Original error: ${e.message}",
                    e
                )
            } catch (e: org.hibernate.StaleObjectStateException) {
                throw AssertionError(
                    "Iteration ${iteration + 1}/5 failed with StaleObjectStateException. " +
                    "This indicates the race condition fix is not working. Original error: ${e.message}",
                    e
                )
            }

            // Then - Verify vedtak was created successfully
            behandlingEtterVedtak.status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
            println("✅ Iteration ${iteration + 1}/5 completed successfully without race condition")
        }

        println("✅ All 5 iterations completed successfully - race condition is fixed!")
    }

    private fun defaultSøknadDto() = SøknadDto(
        søknadsland = SoeknadslandDto(
            land = Land_iso2.DK,
            unntakFraLovvalgsland = false
        ),
        periode = PeriodeDto(
            fom = LocalDate.now(),
            tom = LocalDate.now().plusYears(2)
        ),
        arbeidUtland = false,
        oppholdsperioder = emptyList(),
        aarsak = Behandlingsaarsaker.SØKNAD
    )

    private fun defaultAvklartefaktaDto() = AvklartefaktaDto(
        arbeidssted = Land_iso2.DK,
        arbeidIUtlandetSkattepliktigNorge = false,
        erStatsborgereINorgeSverigeDanmarkEllerFinland = true,
        selvstendiges = emptyList(),
        virksomheter = emptyList(),
        statsborgerskap = listOf(Land_iso2.NO)
    )

    private fun defaultVilkaarDto() = VilkaarDto(
        lovvalgsland = Land_iso2.DK,
        arbeidIUtlandet = true,
        statsborgerskap = listOf(Land_iso2.NO),
        erBosattNorge = true
    )
}
