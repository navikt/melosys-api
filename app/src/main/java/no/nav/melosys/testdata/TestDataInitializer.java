package no.nav.melosys.testdata;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Initialiserer forhåndsdefinerte test-saker i Oracle-databasen ved oppstart.
 * Kjører kun i local-mock profil.
 * <p>
 * MATCHER NØYAKTIG testdataUtils.ts - 56 saker.
 * Hver e2e-test får sin egen dedikerte sak for full isolasjon.
 * <p>
 * MEL-1001 til MEL-1012: opprettAvtalelandSak (12 saker)
 * MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker)
 * MEL-1023 til MEL-1050: opprettUtenforAvtalelandSakMedAarsavregning (28 årsavregning-saker)
 * MEL-1051 til MEL-1053: opprettEUEOSSak IKKE_YRKESAKTIV (3 saker)
 * MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker)
 */
@Component
@Profile("local-mock")
public class TestDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);
    private static final String TEST_FNR = "30056928150";
    private static final String TEST_AKTOR_ID = "1111111111111";
    private static final String TEST_SAKSBEHANDLER = "Z123456";

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    public TestDataInitializer(FagsakRepository fagsakRepository,
                               BehandlingService behandlingService,
                               OppgaveService oppgaveService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("🔄 Initialiserer test-saker (matcher testdataUtils.ts)...");

        // Sjekk om testdata allerede eksisterer (idempotent)
        if (fagsakRepository.existsById("MEL-1001")) {
            log.info("⏭️  Test-saker eksisterer allerede (MEL-1001 funnet) - hopper over initialisering");
            return;
        }

        // Sett ThreadLocalAccessInfo context til test-saksbehandler Z123456
        UUID processId = UUID.randomUUID();
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(processId, "TestDataInitializer", TEST_SAKSBEHANDLER, "Test Saksbehandler");
            log.info("📝 Oppretter 56 test-saker (MEL-1001 til MEL-1056)...");

            // MEL-1001 til MEL-1012: opprettAvtalelandSak (12 saker)
            opprettAvtalelandSak("MEL-1001");
            opprettAvtalelandSak("MEL-1002");
            opprettAvtalelandSak("MEL-1003");
            opprettAvtalelandSak("MEL-1004");
            opprettAvtalelandSak("MEL-1005");
            opprettAvtalelandSak("MEL-1006");
            opprettAvtalelandSak("MEL-1007");
            opprettAvtalelandSak("MEL-1008");
            opprettAvtalelandSak("MEL-1009");
            opprettAvtalelandSak("MEL-1010");
            opprettAvtalelandSak("MEL-1011");
            opprettAvtalelandSakOpprettet("MEL-1012"); // OPPRETTET for behandlingstype-tilgjengelighet test

            // MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker)
            opprettUtenforAvtalelandSak("MEL-1013");
            opprettUtenforAvtalelandSak("MEL-1014");
            opprettUtenforAvtalelandSak("MEL-1015");
            opprettUtenforAvtalelandSak("MEL-1016");
            opprettUtenforAvtalelandSak("MEL-1017");
            opprettUtenforAvtalelandSak("MEL-1018");
            opprettUtenforAvtalelandSak("MEL-1019");
            opprettUtenforAvtalelandSak("MEL-1020");
            opprettUtenforAvtalelandSak("MEL-1021");
            opprettUtenforAvtalelandSak("MEL-1022");

            // MEL-1023 til MEL-1047: opprettUtenforAvtalelandSakMedAarsavregning (25 saker for årsavregning-testene)
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1023");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1024");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1025");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1026");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1027");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1028");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1029");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1030");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1031");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1032");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1033");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1034");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1035");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1036");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1037");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1038");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1039");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1040");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1041");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1042");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1043");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1044");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1045");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1046");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1047");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1048");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1049");
            opprettUtenforAvtalelandSakMedAarsavregning("MEL-1050");

            // MEL-1051 til MEL-1053: opprettEUEOSSak (3 saker)
            opprettEUEOSSak("MEL-1051", Behandlingstema.IKKE_YRKESAKTIV);
            opprettEUEOSSak("MEL-1052", Behandlingstema.IKKE_YRKESAKTIV);
            opprettEUEOSSak("MEL-1053", Behandlingstema.IKKE_YRKESAKTIV);

            // MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker)
            opprettEøsPensjonistSakMedTrygdeavgift("MEL-1054");
            opprettEøsPensjonistSakMedTrygdeavgift("MEL-1055");
            opprettEøsPensjonistSakMedTrygdeavgift("MEL-1056");

            log.info("✅ Suksessfullt initialisert 56 test-saker (MEL-1001 til MEL-1056)");
            log.info("Test-saker tilgjengelig for fnr: {}", TEST_FNR);
        } catch (Exception e) {
            log.error("❌ Feil ved initialisering av test-saker: {}", e.getMessage(), e);
        } finally {
            // Rydd opp ThreadLocalAccessInfo
            ThreadLocalAccessInfo.afterExecuteProcess(processId);
        }
    }

    /**
     * testdataUtils.ts: opprettAvtalelandSak()
     * <p>
     * velgSakstype("Avtaleland")
     * velgSakstema("Medlemskap og lovvalg")
     * velgBehandlingstema("Yrkesaktiv")
     * velgBehandlingstype("Førstegangsbehandling")
     * velgBehandlingsaarsak("Søknad")
     */
    private void opprettAvtalelandSak(String saksnummer) {
        opprettAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.UNDER_BEHANDLING);
    }

    /**
     * Oppretter Avtaleland-sak med OPPRETTET status (ikke redigerbar)
     * Brukes av tester som søker etter saker med "Behandlingen er opprettet"
     */
    private void opprettAvtalelandSakOpprettet(String saksnummer) {
        opprettAvtalelandSakMedStatus(saksnummer, Behandlingsstatus.OPPRETTET);
    }

    private void opprettAvtalelandSakMedStatus(String saksnummer, Behandlingsstatus status) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer);
            return;
        }

        Fagsak fagsak = new Fagsak(
            saksnummer,
            null,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            new java.util.HashSet<>(),
            new java.util.ArrayList<>()
        );

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(fagsak);
        bruker.setPersonIdent(TEST_FNR);
        bruker.setAktørId(TEST_AKTOR_ID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(bruker);

        Fagsak savedFagsak = fagsakRepository.save(fagsak);

        Behandling behandling = behandlingService.nyBehandling(
            savedFagsak,
            status,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // Opprett oppgave tilordnet test-saksbehandler for redigerbare behandlinger
        if (status == Behandlingsstatus.UNDER_BEHANDLING) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null);
        }

        log.info("✅ {}: opprettAvtalelandSak (status: {})", saksnummer, status);
    }

    /**
     * testdataUtils.ts: opprettUtenforAvtalelandSak()
     * <p>
     * velgSakstype("Utenfor avtaleland")
     * velgSakstema("Medlemskap og lovvalg")
     * velgBehandlingstema("Yrkesaktiv")
     * velgBehandlingstype("Førstegangsbehandling")
     * velgBehandlingsaarsak("Søknad")
     */
    private void opprettUtenforAvtalelandSak(String saksnummer) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer);
            return;
        }

        Fagsak fagsak = new Fagsak(
            saksnummer,
            null,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            new java.util.HashSet<>(),
            new java.util.ArrayList<>()
        );

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(fagsak);
        bruker.setPersonIdent(TEST_FNR);
        bruker.setAktørId(TEST_AKTOR_ID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(bruker);

        Fagsak savedFagsak = fagsakRepository.save(fagsak);

        Behandling behandling = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null);

        log.info("✅ {}: opprettUtenforAvtalelandSak", saksnummer);
    }

    /**
     * testdataUtils.ts: opprettEøsPensjonistSakMedTrygdeavgift()
     * <p>
     * velgSakstype("EU/EØS-land")
     * velgSakstema("Trygdeavgift")
     * velgBehandlingstema("Pensjonist/uføretrygdet")
     * velgBehandlingstype("Førstegangsbehandling")
     * velgBehandlingsaarsak("Søknad")
     */
    private void opprettEøsPensjonistSakMedTrygdeavgift(String saksnummer) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer);
            return;
        }

        Fagsak fagsak = new Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Saksstatuser.OPPRETTET,
            null,
            new java.util.HashSet<>(),
            new java.util.ArrayList<>()
        );

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(fagsak);
        bruker.setPersonIdent(TEST_FNR);
        bruker.setAktørId(TEST_AKTOR_ID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(bruker);

        Fagsak savedFagsak = fagsakRepository.save(fagsak);

        Behandling behandling = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.PENSJONIST,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null);

        log.info("✅ {}: opprettEøsPensjonistSakMedTrygdeavgift", saksnummer);
    }

    /**
     * testdataUtils.ts: opprettEUEOSSak()
     * <p>
     * velgSakstype("EU/EØS-land")
     * velgSakstema("Medlemskap og lovvalg")
     * velgBehandlingstema(behandlingstema) // parameterisert
     * velgBehandlingstype("Førstegangsbehandling")
     * velgBehandlingsaarsak("Søknad")
     */
    private void opprettEUEOSSak(String saksnummer, Behandlingstema behandlingstema) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer);
            return;
        }

        Fagsak fagsak = new Fagsak(
            saksnummer,
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            new java.util.HashSet<>(),
            new java.util.ArrayList<>()
        );

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(fagsak);
        bruker.setPersonIdent(TEST_FNR);
        bruker.setAktørId(TEST_AKTOR_ID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(bruker);

        Fagsak savedFagsak = fagsakRepository.save(fagsak);

        Behandling behandling = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null);

        log.info("✅ {}: opprettEUEOSSak ({})", saksnummer, behandlingstema);
    }

    /**
     * testdataUtils.ts: opprettUtenforAvtalelandSakMedAarsavregning()
     * <p>
     * Oppretter en FTRL-sak med kun en årsavregning-behandling (OPPRETTET).
     * <p>
     * Forenklet versjon - testene trenger ikke historisk førstegangsbehandling,
     * bare en åpen årsavregning å teste mot.
     */
    private void opprettUtenforAvtalelandSakMedAarsavregning(String saksnummer) {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug("Sak {} eksisterer allerede", saksnummer);
            return;
        }

        // 1. Opprett FTRL-sak
        Fagsak fagsak = new Fagsak(
            saksnummer,
            null,
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            new java.util.HashSet<>(),
            new java.util.ArrayList<>()
        );

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(fagsak);
        bruker.setPersonIdent(TEST_FNR);
        bruker.setAktørId(TEST_AKTOR_ID);
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.leggTilAktør(bruker);

        Fagsak savedFagsak = fagsakRepository.save(fagsak);

        // 2. Opprett Årsavregning-behandling (åpen)
        Behandling behandling = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.ÅRSAVREGNING,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // 3. Opprett oppgave tilordnet test-saksbehandler
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null);

        log.info("✅ {}: opprettUtenforAvtalelandSakMedAarsavregning (1 årsavregning)", saksnummer);
    }
}
