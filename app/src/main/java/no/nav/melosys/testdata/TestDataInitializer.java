package no.nav.melosys.testdata;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

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

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final TransactionTemplate transactionTemplate;

    public TestDataInitializer(FagsakRepository fagsakRepository,
                               BehandlingService behandlingService,
                               OppgaveService oppgaveService,
                               PlatformTransactionManager transactionManager) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initialiserer test-saker (matcher testdataUtils.ts)...");

        try {
            // MEL-1001 til MEL-1012: opprettAvtalelandSak (12 saker)
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1001"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1002"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1003"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1004"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1005"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1006"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1007"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1008"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1009"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1010"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1011"));
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1012"));

            // MEL-1013 til MEL-1022: opprettUtenforAvtalelandSak (10 saker)
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1013"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1014"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1015"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1016"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1017"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1018"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1019"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1020"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1021"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1022"));

            // MEL-1023 til MEL-1047: opprettUtenforAvtalelandSakMedAarsavregning (25 saker for årsavregning-testene)
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1023"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1024"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1025"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1026"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1027"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1028"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1029"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1030"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1031"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1032"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1033"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1034"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1035"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1036"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1037"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1038"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1039"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1040"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1041"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1042"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1043"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1044"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1045"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1046"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1047"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1048"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1049"));
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1050"));

            // MEL-1051 til MEL-1053: opprettEUEOSSak (3 saker)
            transactionTemplate.executeWithoutResult(status -> opprettEUEOSSak("MEL-1051", Behandlingstema.IKKE_YRKESAKTIV));
            transactionTemplate.executeWithoutResult(status -> opprettEUEOSSak("MEL-1052", Behandlingstema.IKKE_YRKESAKTIV));
            transactionTemplate.executeWithoutResult(status -> opprettEUEOSSak("MEL-1053", Behandlingstema.IKKE_YRKESAKTIV));

            // MEL-1054 til MEL-1056: opprettEøsPensjonistSakMedTrygdeavgift (3 saker)
            transactionTemplate.executeWithoutResult(status -> opprettEøsPensjonistSakMedTrygdeavgift("MEL-1054"));
            transactionTemplate.executeWithoutResult(status -> opprettEøsPensjonistSakMedTrygdeavgift("MEL-1055"));
            transactionTemplate.executeWithoutResult(status -> opprettEøsPensjonistSakMedTrygdeavgift("MEL-1056"));

            log.info("✅ Suksessfullt initialisert 56 test-saker (MEL-1001 til MEL-1056)");
            log.info("Test-saker tilgjengelig for fnr: {}", TEST_FNR);
        } catch (Exception e) {
            log.error("❌ Feil ved initialisering av test-saker: {}", e.getMessage(), e);
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

        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        log.info("✅ {}: opprettAvtalelandSak", saksnummer);
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

        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

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

        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.PENSJONIST,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

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

        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.FØRSTEGANG,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        log.info("✅ {}: opprettEUEOSSak ({})", saksnummer, behandlingstema);
    }

    /**
     * testdataUtils.ts: opprettUtenforAvtalelandSakMedAarsavregning()
     * <p>
     * Oppretter en FTRL-sak med kun en årsavregning-behandling (OPPRETTET) med oppgave.
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
        Behandling aarsavregning = behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.ÅRSAVREGNING,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // 3. Opprett oppgave for årsavregningen (påkrevd for at behandlingen skal være tilgjengelig)
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            aarsavregning,
            null,  // journalpostID
            TEST_AKTOR_ID,  // aktørID
            null,  // tilordnetRessurs (ikke tildelt noen saksbehandler)
            null   // orgnr
        );

        log.info("✅ {}: opprettUtenforAvtalelandSakMedAarsavregning (1 årsavregning med oppgave)", saksnummer);
    }
}
