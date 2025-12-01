package no.nav.melosys.testdata

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*

/**
 * Initialiserer forhåndsdefinerte test-saker i Oracle-databasen ved oppstart.
 * Kjører kun i local-mock-testdata profil (som automatisk aktiverer local-mock).
 *
 * VIKTIG: Disse sakene brukes av frontend Playwright e2e-tester (testdataUtils.ts).
 * Hver e2e-test får sin egen dedikerte sak for full isolasjon.
 * Endringer her må synkroniseres med frontend testdataUtils.ts!
 *
 * MEL-1001 til MEL-1011: opprettAvtalelandSak (11 saker - UNDER_BEHANDLING)
 * MEL-1012: opprettAvtalelandSak (1 sak - OPPRETTET for behandlingstype-tilgjengelighet test)
 * MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker - UNDER_BEHANDLING)
 * MEL-1023 til MEL-1050: opprettUtenforAvtalelandSakMedAarsavregning (28 saker - UNDER_BEHANDLING)
 * MEL-1051 til MEL-1053: opprettEUEOSSak IKKE_YRKESAKTIV (3 saker - UNDER_BEHANDLING)
 * MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker - UNDER_BEHANDLING)
 * MEL-1057 til MEL-1062: OPPRETTET saker for "knytt til eksisterende" tester (6 saker)
 * MEL-1063 til MEL-1068: AVSLUTTET saker for "knytt til eksisterende" tester (6 saker)
 * MEL-1069: Avtaleland med åpen behandling for varselmelding-test (1 sak - UNDER_BEHANDLING)
 * MEL-1070: Utenfor avtaleland AVSLUTTET for behandlingstype-tilgjengelighet test (1 sak - AVSLUTTET)
 * MEL-1071: EU_EOS IKKE_YRKESAKTIV for regresjon-state-ved-saksbytting test (1 sak - UNDER_BEHANDLING)
 */
@Component
@Profile("local-mock-testdata")
class TestDataInitializer(
    private val fagsakRepository: FagsakRepository,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        // Sjekk om testdata allerede eksisterer (idempotent)
        // Sjekker første og siste sak i området for å fange opp både fullstendig og delvis initialisering
        if (fagsakRepository.existsById("MEL-1001") && fagsakRepository.existsById("MEL-1071")) {
            log.info("⏭️  Test-saker eksisterer allerede (MEL-1001 til MEL-1071) - hopper over initialisering")
            return
        }

        log.info("🔄 Initialiserer test-saker (MEL-1001 til MEL-1071)...")

        // Sett ThreadLocalAccessInfo context til test-saksbehandler Z123456
        val processId = UUID.randomUUID()
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(processId, "TestDataInitializer", TEST_SAKSBEHANDLER, "Test Saksbehandler")

            // MEL-1001 til MEL-1011: opprettAvtalelandSak (11 saker - UNDER_BEHANDLING)
            // MEL-1012: opprettAvtalelandSak (1 sak - OPPRETTET for behandlingstype-tilgjengelighet test)
            (1001..1011).forEach { opprettAvtalelandSak("MEL-$it") }
            opprettAvtalelandSak("MEL-1012", Behandlingsstatus.OPPRETTET)

            // MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker)
            (1013..1022).forEach { opprettUtenforAvtalelandSak("MEL-$it") }

            // MEL-1023 til MEL-1050: opprettUtenforAvtalelandSakMedAarsavregning (28 saker)
            (1023..1050).forEach { opprettUtenforAvtalelandSakMedAarsavregning("MEL-$it") }

            // MEL-1051 til MEL-1053: opprettEUEOSSak (3 saker)
            (1051..1053).forEach { opprettEUEOSSak("MEL-$it", Behandlingstema.IKKE_YRKESAKTIV) }

            // MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker)
            (1054..1056).forEach { opprettEøsPensjonistSakMedTrygdeavgift("MEL-$it") }

            // MEL-1057 til MEL-1062: OPPRETTET saker for "knytt til eksisterende" tester
            (1057..1058).forEach { opprettAvtalelandSak("MEL-$it", Behandlingsstatus.OPPRETTET) }
            (1059..1060).forEach { opprettUtenforAvtalelandSak("MEL-$it", Behandlingsstatus.OPPRETTET) }
            opprettEUEOSSak("MEL-1061", Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.OPPRETTET)
            opprettEøsPensjonistSakMedTrygdeavgift("MEL-1062", Behandlingsstatus.OPPRETTET)

            // MEL-1063 til MEL-1068: AVSLUTTET saker for "knytt til eksisterende" tester
            opprettAvtalelandSak("MEL-1063", Behandlingsstatus.AVSLUTTET, Saksstatuser.HENLAGT)
            opprettAvtalelandSak("MEL-1064", Behandlingsstatus.AVSLUTTET)
            (1065..1066).forEach { opprettUtenforAvtalelandSak("MEL-$it", Behandlingsstatus.AVSLUTTET) }
            opprettEUEOSSak("MEL-1067", Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.AVSLUTTET)
            opprettEøsPensjonistSakMedTrygdeavgift("MEL-1068", Behandlingsstatus.AVSLUTTET)

            // MEL-1069: Avtaleland med åpen behandling for test av varselmelding
            opprettAvtalelandSak("MEL-1069")

            // MEL-1070: Utenfor avtaleland AVSLUTTET for behandlingstype-tilgjengelighet test
            opprettUtenforAvtalelandSak("MEL-1070", Behandlingsstatus.AVSLUTTET)

            // MEL-1071: EU_EOS IKKE_YRKESAKTIV for regresjon-state-ved-saksbytting test
            opprettEUEOSSak("MEL-1071", Behandlingstema.IKKE_YRKESAKTIV)

            log.info("✅ Suksessfullt initialisert test-saker for testperson: {}", TEST_FNR)
        } catch (e: Exception) {
            log.error("❌ Feil ved initialisering av test-saker: {}", e.message, e)
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    // ===== Generisk sak-opprettelse =====

    /**
     * Generisk metode for å opprette en test-sak med førstegangsbehandling.
     * Alle andre opprett-metoder delegerer til denne.
     */
    private fun opprettSak(
        saksnummer: String,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingsstatus: Behandlingsstatus,
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
        saksstatus: Saksstatuser = Saksstatuser.OPPRETTET
    ) {
        if (sakEksisterer(saksnummer)) return

        val fagsak = Fagsak(
            saksnummer,
            null,
            sakstype,
            sakstema,
            saksstatus,
            null,
            HashSet(),
            ArrayList()
        )

        val bruker = Aktoer().apply {
            setFagsak(fagsak)
            personIdent = TEST_FNR
            aktørId = TEST_AKTOR_ID
            rolle = Aktoersroller.BRUKER
        }
        fagsak.leggTilAktør(bruker)

        val savedFagsak = fagsakRepository.save(fagsak)

        val behandling = behandlingService.nyBehandling(
            savedFagsak,
            behandlingsstatus,
            behandlingstype,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (behandlingsstatus != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)
        }

        log.debug("✅ {}: {} / {} / {} (status: {})", saksnummer, sakstype, sakstema, behandlingstema, behandlingsstatus)
    }

    // ===== Semantiske helper-metoder =====

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettAvtalelandSak()
     *
     * Playwright-testen bruker:
     * - velgSakstype("Avtaleland")
     * - velgSakstema("Medlemskap og lovvalg")
     * - velgBehandlingstema("Yrkesaktiv")
     * - velgBehandlingstype("Førstegangsbehandling")
     * - velgBehandlingsaarsak("Søknad")
     */
    private fun opprettAvtalelandSak(
        saksnummer: String,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
        saksstatus: Saksstatuser = Saksstatuser.OPPRETTET
    ) {
        opprettSak(
            saksnummer = saksnummer,
            sakstype = Sakstyper.TRYGDEAVTALE,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandlingstema = Behandlingstema.YRKESAKTIV,
            behandlingsstatus = behandlingsstatus,
            saksstatus = saksstatus
        )
    }

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettUtenforAvtalelandSak()
     *
     * Playwright-testen bruker:
     * - velgSakstype("Utenfor avtaleland")
     * - velgSakstema("Medlemskap og lovvalg")
     * - velgBehandlingstema("Yrkesaktiv")
     * - velgBehandlingstype("Førstegangsbehandling")
     * - velgBehandlingsaarsak("Søknad")
     */
    private fun opprettUtenforAvtalelandSak(
        saksnummer: String,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING
    ) {
        opprettSak(
            saksnummer = saksnummer,
            sakstype = Sakstyper.FTRL,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandlingstema = Behandlingstema.YRKESAKTIV,
            behandlingsstatus = behandlingsstatus
        )
    }

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettUtenforAvtalelandSakMedAarsavregning()
     *
     * Oppretter en FTRL-sak med kun en årsavregning-behandling.
     */
    private fun opprettUtenforAvtalelandSakMedAarsavregning(saksnummer: String) {
        opprettSak(
            saksnummer = saksnummer,
            sakstype = Sakstyper.FTRL,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandlingstema = Behandlingstema.YRKESAKTIV,
            behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
            behandlingstype = Behandlingstyper.ÅRSAVREGNING
        )
    }

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettEUEOSSak()
     *
     * Playwright-testen bruker:
     * - velgSakstype("EU/EØS-land")
     * - velgSakstema("Medlemskap og lovvalg")
     * - velgBehandlingstema(behandlingstema) // parameterisert
     * - velgBehandlingstype("Førstegangsbehandling")
     * - velgBehandlingsaarsak("Søknad")
     */
    private fun opprettEUEOSSak(
        saksnummer: String,
        behandlingstema: Behandlingstema,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING
    ) {
        opprettSak(
            saksnummer = saksnummer,
            sakstype = Sakstyper.EU_EOS,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandlingstema = behandlingstema,
            behandlingsstatus = behandlingsstatus
        )
    }

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettEøsPensjonistSakMedTrygdeavgift()
     *
     * Playwright-testen bruker:
     * - velgSakstype("EU/EØS-land")
     * - velgSakstema("Trygdeavgift")
     * - velgBehandlingstema("Pensjonist/uføretrygdet")
     * - velgBehandlingstype("Førstegangsbehandling")
     * - velgBehandlingsaarsak("Søknad")
     */
    private fun opprettEøsPensjonistSakMedTrygdeavgift(
        saksnummer: String,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING
    ) {
        opprettSak(
            saksnummer = saksnummer,
            sakstype = Sakstyper.EU_EOS,
            sakstema = Sakstemaer.TRYGDEAVGIFT,
            behandlingstema = Behandlingstema.PENSJONIST,
            behandlingsstatus = behandlingsstatus
        )
    }

    /**
     * Idempotency-sjekk: returnerer true hvis saken allerede eksisterer.
     */
    private fun sakEksisterer(saksnummer: String): Boolean {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return true
        }
        return false
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestDataInitializer::class.java)
        private const val TEST_FNR = "30056928150"
        private const val TEST_AKTOR_ID = "1111111111111"
        private const val TEST_SAKSBEHANDLER = "Z123456"
    }
}
