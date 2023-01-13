package no.nav.melosys.itest

import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.featuretoggle.LocalUnleash
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(OAuthMockServer::class, LocalUnleash::class)
class MottatteOpplysningerIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val oAuthMockServer: OAuthMockServer
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        unleash.resetAll()
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
    }

    @Test
    fun `journalførOgOpprettSak for uten melosys-folketrygden-mvp toggle og så opprettSøknad  med togge`() {
        unleash.enable(ToggleName.BEHANDLINGSTYPE_KLAGE)

        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.FTRL.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
        }

        val journalføringProsess = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto,
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        )

        val behandling = journalføringProsess.behandling

        unleash.enable(ToggleName.FOLKETRYGDEN_MVP)
        // Denne feiler med InvalidDataAccessApiUsageException
        mottatteOpplysningerService.opprettSøknad(behandling, Periode(), Soeknadsland())
    }
}
