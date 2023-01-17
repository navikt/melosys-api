package no.nav.melosys.itest

import io.kotest.matchers.optional.shouldBeEmpty
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.mottatteopplysninger.FTRLMottatteOpplysningerService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate

@ActiveProfiles("test")
@Import(FakeUnleash::class, FTRLMottatteOpplysningerService::class)
@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MottatteOpplysningerUtenFullContextIT(
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val mottatteOpplysningerRepository: MottatteOpplysningerRepository,
    @Autowired private val ftrlMottatteOpplysningerService: FTRLMottatteOpplysningerService,
    @Autowired private val unleash: FakeUnleash,
) : DataJpaTestBase() {

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    private val behandlingService by lazy {
        BehandlingService(
            behandlingRepository, null, null,
            null, null, null,
            UtledMottaksdato(joarkFasade), unleash
        )
    }

    private val mottatteOpplysningerService by lazy {
        MottatteOpplysningerService(
            mottatteOpplysningerRepository,
            ftrlMottatteOpplysningerService,
            behandlingService, joarkFasade, unleash
        )
    }

    @BeforeAll
    fun before() {
        every { joarkFasade.hentMottaksDatoForJournalpost(any()) } returns LocalDate.now()
    }

    @Test
    fun `legg til mottate opplysinger på eksisterende behanding`() {
        unleash.enable(ToggleName.FOLKETRYGDEN_MVP)

        val behandling = lagFagsakMedBehandlinge()

        mottatteOpplysningerService.hentMottatteOpplysninger(behandling.id)

        // skal ikke lages ved andre kjøring
        mottatteOpplysningerService.hentMottatteOpplysninger(behandling.id)
    }

    @Test
    fun `legg til mottate opplysinger på eksisterende behanding 2`() {
        val behandling = lagFagsakMedBehandlinge()

        mottatteOpplysningerRepository.findById(behandling.id).shouldBeEmpty()
    }

    private fun lagFagsakMedBehandlinge(): Behandling {
        Fagsak().apply {
            saksnummer = "MEL-1001"
            type = Sakstyper.FTRL
            status = Saksstatuser.OPPRETTET
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            leggTilRegisteringInfo()
        }.also { fsak ->
            fagsakRepository.save(fsak)

            return Behandling().apply {
                fagsak = fsak
                leggTilRegisteringInfo()
                behandlingsfrist = LocalDate.now().plusYears(1)
                status = Behandlingsstatus.OPPRETTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                initierendeJournalpostId = "1223"

            }.also { behandlingRepository.save(it) }
        }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
