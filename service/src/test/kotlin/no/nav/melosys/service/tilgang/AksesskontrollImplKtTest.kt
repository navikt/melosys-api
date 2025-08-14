package no.nav.melosys.service.tilgang

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import no.nav.melosys.sikkerhet.logging.AuditEvent
import no.nav.melosys.sikkerhet.logging.AuditLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AksesskontrollImplKtTest {

    @SpyK
    private var auditLogger = AuditLogger()

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var brukertilgangKontroll: BrukertilgangKontroll

    @MockK
    private lateinit var redigerbarKontroll: RedigerbarKontroll

    @MockK
    private lateinit var oppgaveService: OppgaveService

    private lateinit var aksesskontroll: Aksesskontroll

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private val behandlingID = 1111L

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        fagsak = FagsakTestFactory.builder()
            .medBruker()
            .build()
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .medFagsak(fagsak)
            .build()

        SubjectHandler.set(TestSubjectHandler())

        aksesskontroll = AksesskontrollImpl(auditLogger, fagsakService, behandlingService, brukertilgangKontroll, redigerbarKontroll, oppgaveService)
    }

    @Test
    fun `auditAutoriserFolkeregisterIdent skal audit og sjekke tilgang`() {
        val captor = slot<AuditEvent>()
        every { auditLogger.log(capture(captor)) } returns Unit
        every { brukertilgangKontroll.validerTilgangTilFolkeregisterIdent(any()) } returns Unit

        aksesskontroll.auditAutoriserFolkeregisterIdent("fnr", "melding")

        verify { auditLogger.log(any()) }
        verify { brukertilgangKontroll.validerTilgangTilFolkeregisterIdent("fnr") }
        captor.captured.run {
            sourceUserId.shouldNotBeNull()
            destinationUserId shouldBe "fnr"
            message shouldBe "melding"
        }
    }

    @Test
    fun `auditAutoriserSakstilgang skal audit og sjekke tilgang`() {
        val captor = slot<AuditEvent>()
        every { auditLogger.log(capture(captor)) } returns Unit
        every { brukertilgangKontroll.validerTilgangTilAktørID(any()) } returns Unit

        aksesskontroll.auditAutoriserSakstilgang(fagsak, "melding")

        verify { auditLogger.log(any()) }
        verify { brukertilgangKontroll.validerTilgangTilAktørID(fagsak.finnBrukersAktørID()) }
        captor.captured.run {
            sourceUserId.shouldNotBeNull()
            destinationUserId shouldBe fagsak.finnBrukersAktørID()
            message shouldBe "melding"
        }
    }

    @Test
    fun `autoriserSakstilgang skal sjekke bruker`() {
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { brukertilgangKontroll.validerTilgangTilAktørID(any()) } returns Unit

        aksesskontroll.autoriserSakstilgang(SAKSNUMMER)

        verify { brukertilgangKontroll.validerTilgangTilAktørID(BRUKER_AKTØR_ID) }
    }

    @Test
    fun `autoriser skal verifisere sjekk lesetilgang`() {
        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { brukertilgangKontroll.validerTilgangTilAktørID(any()) } returns Unit

        aksesskontroll.autoriser(behandlingID)

        verify { brukertilgangKontroll.validerTilgangTilAktørID(BRUKER_AKTØR_ID) }
        verify(exactly = 0) { redigerbarKontroll.sjekkRessursRedigerbar(any(), any()) }
    }

    @Test
    fun `autoriser med skalSkrive skal verifisere redigerbar behandling`() {
        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { brukertilgangKontroll.validerTilgangTilAktørID(any()) } returns Unit
        every { redigerbarKontroll.sjekkRessursRedigerbar(any(), any()) } returns Unit

        aksesskontroll.autoriser(behandlingID, Aksesstype.SKRIV)

        verify { brukertilgangKontroll.validerTilgangTilAktørID(BRUKER_AKTØR_ID) }
        verify { redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT) }
    }

    @Test
    fun `autoriser uten bruker skal ikke sjekke aktørID`() {
        behandling.fagsak = FagsakTestFactory.builder().medVirksomhet().build()
        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { redigerbarKontroll.sjekkRessursRedigerbar(any(), any()) } returns Unit

        aksesskontroll.autoriser(behandlingID, Aksesstype.SKRIV)

        verify(exactly = 0) { brukertilgangKontroll.validerTilgangTilAktørID(any()) }
        verify { redigerbarKontroll.sjekkRessursRedigerbar(behandling, Ressurs.UKJENT) }
    }

    @Test
    fun `autoriserSkrivTilRessurs skal verifisere at redigerbar behandling sjekkes`() {
        val skrivTilRessurs = Ressurs.AVKLARTE_FAKTA
        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { brukertilgangKontroll.validerTilgangTilAktørID(any()) } returns Unit
        every { redigerbarKontroll.sjekkRessursRedigerbar(any(), any()) } returns Unit

        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, skrivTilRessurs)

        verify { brukertilgangKontroll.validerTilgangTilAktørID(BRUKER_AKTØR_ID) }
        verify { redigerbarKontroll.sjekkRessursRedigerbar(behandling, skrivTilRessurs) }
    }

    @Test
    fun `behandlingKanRedigeresAvSaksbehandler med ikke-redigerbar behandling skal returnere false`() {
        behandling.status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        every { redigerbarKontroll.behandlingErRedigerbar(behandling) } returns false

        val saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, "Z123")

        saksbehandlerHarTilgang shouldBe false
    }

    @Test
    fun `behandlingKanRedigeresAvSaksbehandler med redigerbar behandling men ikke-tilordnet oppgave skal returnere false`() {
        val saksbehandler = "Z111111"
        every { redigerbarKontroll.behandlingErRedigerbar(behandling) } returns true
        every { oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandlingID) } returns false

        val saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler)

        saksbehandlerHarTilgang shouldBe false
    }

    @Test
    fun `behandlingKanRedigeresAvSaksbehandler med redigerbar behandling og tilordnet oppgave skal returnere true`() {
        val saksbehandler = "Z111111"
        every { redigerbarKontroll.behandlingErRedigerbar(behandling) } returns true
        every { oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandlingID) } returns true

        val saksbehandlerHarTilgang = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler)

        saksbehandlerHarTilgang shouldBe true
    }
}
