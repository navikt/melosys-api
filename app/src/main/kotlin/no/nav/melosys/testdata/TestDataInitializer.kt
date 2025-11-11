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
 * MEL-1001 til MEL-1012: opprettAvtalelandSak (12 saker - UNDER_BEHANDLING)
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
        if (fagsakRepository.existsById("MEL-1068")) {
            log.info("⏭️  Test-saker eksisterer allerede - hopper over initialisering")
            return
        }

        log.info("🔄 Initialiserer test-saker (MEL-1001 til MEL-1071)...")

        // Sett ThreadLocalAccessInfo context til test-saksbehandler Z123456
        val processId = UUID.randomUUID()
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(processId, "TestDataInitializer", TEST_SAKSBEHANDLER, "Test Saksbehandler")

            // MEL-1001 til MEL-1012: opprettAvtalelandSak (12 saker)
            (1001..1011).forEach { opprettAvtalelandSak("MEL-$it") }
            opprettAvtalelandSakOpprettet("MEL-1012") // OPPRETTET for behandlingstype-tilgjengelighet test

            // MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker)
            (1013..1022).forEach { opprettUtenforAvtalelandSak("MEL-$it") }

            // MEL-1023 til MEL-1050: opprettUtenforAvtalelandSakMedAarsavregning (28 saker)
            (1023..1050).forEach { opprettUtenforAvtalelandSakMedAarsavregning("MEL-$it") }

            // MEL-1051 til MEL-1053: opprettEUEOSSak (3 saker)
            (1051..1053).forEach { opprettEUEOSSak("MEL-$it", Behandlingstema.IKKE_YRKESAKTIV) }

            // MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker)
            (1054..1056).forEach { opprettEøsPensjonistSakMedTrygdeavgift("MEL-$it") }

            // MEL-1057 til MEL-1062: OPPRETTET saker for "knytt til eksisterende" tester
            (1057..1058).forEach { opprettAvtalelandSakOpprettet("MEL-$it") }
            (1059..1060).forEach { opprettUtenforAvtalelandSakOpprettet("MEL-$it") }
            opprettEUEOSSakMedStatus("MEL-1061", Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.OPPRETTET)
            opprettEøsPensjonistSakMedTrygdeavgiftMedStatus("MEL-1062", Behandlingsstatus.OPPRETTET)

            // MEL-1063 til MEL-1068: AVSLUTTET saker for "knytt til eksisterende" tester
            opprettAvtalelandSakMedStatus("MEL-1063", Behandlingsstatus.AVSLUTTET, Saksstatuser.HENLAGT) // HENLAGT
            opprettAvtalelandSakMedStatus("MEL-1064", Behandlingsstatus.AVSLUTTET, Saksstatuser.OPPRETTET)
            (1065..1066).forEach { opprettUtenforAvtalelandSakAvsluttet("MEL-$it") }
            opprettEUEOSSakMedStatus("MEL-1067", Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.AVSLUTTET)
            opprettEøsPensjonistSakMedTrygdeavgiftMedStatus("MEL-1068", Behandlingsstatus.AVSLUTTET)

            // MEL-1069: Avtaleland med åpen behandling for test av varselmelding
            opprettAvtalelandSak("MEL-1069")

            // MEL-1070: Utenfor avtaleland AVSLUTTET for behandlingstype-tilgjengelighet test
            opprettUtenforAvtalelandSakAvsluttet("MEL-1070")

            // MEL-1071: EU_EOS IKKE_YRKESAKTIV for regresjon-state-ved-saksbytting test
            opprettEUEOSSak("MEL-1071", Behandlingstema.IKKE_YRKESAKTIV)

            log.info("✅ Suksessfullt initialisert test-saker for testperson: {}", TEST_FNR)
        } catch (e: Exception) {
            log.error("❌ Feil ved initialisering av test-saker: {}", e.message, e)
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

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
    private fun opprettAvtalelandSak(saksnummer: String) {
        opprettAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.UNDER_BEHANDLING)
    }

    /**
     * Oppretter Avtaleland-sak med OPPRETTET status (ikke redigerbar).
     * Brukes av Playwright-tester som søker etter saker med "Behandlingen er opprettet".
     */
    private fun opprettAvtalelandSakOpprettet(saksnummer: String) {
        opprettAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.OPPRETTET)
    }

    private fun opprettAvtalelandSakMedStatus(saksnummer: String, status: Behandlingsstatus) {
        opprettAvtalelandSakMedStatus(saksnummer, status, Saksstatuser.OPPRETTET)
    }

    private fun opprettAvtalelandSakMedStatus(
        saksnummer: String,
        status: Behandlingsstatus,
        saksstatus: Saksstatuser
    ) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
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
            status,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (status != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)
        }

        log.debug("✅ {}: opprettAvtalelandSak (status: {})", saksnummer, status)
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
    private fun opprettUtenforAvtalelandSak(saksnummer: String) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
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
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)

        log.debug("✅ {}: opprettUtenforAvtalelandSak", saksnummer)
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
    private fun opprettEøsPensjonistSakMedTrygdeavgift(saksnummer: String) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Saksstatuser.OPPRETTET,
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
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.PENSJONIST,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)

        log.debug("✅ {}: opprettEøsPensjonistSakMedTrygdeavgift", saksnummer)
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
    private fun opprettEUEOSSak(saksnummer: String, behandlingstema: Behandlingstema) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
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
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)

        log.debug("✅ {}: opprettEUEOSSak ({})", saksnummer, behandlingstema)
    }

    /**
     * Korrelerer med frontend testdataUtils.ts: opprettUtenforAvtalelandSakMedAarsavregning()
     *
     * Oppretter en FTRL-sak med kun en årsavregning-behandling (OPPRETTET).
     *
     * Forenklet versjon - Playwright-testene trenger ikke historisk førstegangsbehandling,
     * bare en åpen årsavregning å teste mot.
     */
    private fun opprettUtenforAvtalelandSakMedAarsavregning(saksnummer: String) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        // 1. Opprett FTRL-sak
        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
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

        // 2. Opprett Årsavregning-behandling (åpen)
        val behandling = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.ÅRSAVREGNING,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // 3. Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)

        log.debug("✅ {}: opprettUtenforAvtalelandSakMedAarsavregning (1 årsavregning)", saksnummer)
    }

    // ===== Helper-metoder for OPPRETTET og AVSLUTTET saker =====

    private fun opprettUtenforAvtalelandSakOpprettet(saksnummer: String) {
        opprettUtenforAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.OPPRETTET)
    }

    private fun opprettUtenforAvtalelandSakAvsluttet(saksnummer: String) {
        opprettUtenforAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.AVSLUTTET)
    }

    private fun opprettUtenforAvtalelandSakMedStatus(saksnummer: String, status: Behandlingsstatus) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
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
            status,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (status != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)
        }

        log.debug("✅ {}: opprettUtenforAvtalelandSak (status: {})", saksnummer, status)
    }

    private fun opprettEUEOSSakMedStatus(
        saksnummer: String,
        behandlingstema: Behandlingstema,
        status: Behandlingsstatus
    ) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
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
            status,
            Behandlingstyper.FØRSTEGANG,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (status != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)
        }

        log.debug("✅ {}: opprettEUEOSSak (status: {}, tema: {})", saksnummer, status, behandlingstema)
    }

    private fun opprettEøsPensjonistSakMedTrygdeavgiftMedStatus(saksnummer: String, status: Behandlingsstatus) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer)
            return
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Saksstatuser.OPPRETTET,
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
            status,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.PENSJONIST,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (status != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null)
        }

        log.debug("✅ {}: opprettEøsPensjonistSakMedTrygdeavgift (status: {})", saksnummer, status)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestDataInitializer::class.java)
        private const val TEST_FNR = "30056928150"
        private const val TEST_AKTOR_ID = "1111111111111"
        private const val TEST_SAKSBEHANDLER = "Z123456"
    }
}
