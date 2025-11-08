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
 * MATCHER NØYAKTIG testdataUtils.ts - 5 metoder, 6 saker.
 * MEL-6 krever spesialhåndtering (2 behandlinger: avsluttet + årsavregning).
 */
@Component
@Profile("local-mock")
public class TestDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);
    private static final String TEST_FNR = "30056928150";
    private static final String TEST_AKTOR_ID = "1111111111111";

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final TransactionTemplate transactionTemplate;

    public TestDataInitializer(FagsakRepository fagsakRepository,
                               BehandlingService behandlingService,
                               PlatformTransactionManager transactionManager) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initialiserer test-saker (matcher testdataUtils.ts)...");

        try {
            // MEL-1001: opprettAvtalelandSak
            transactionTemplate.executeWithoutResult(status -> opprettAvtalelandSak("MEL-1001"));

            // MEL-1002: opprettUtenforAvtalelandSak
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSak("MEL-1002"));

            // MEL-1003: opprettEøsPensjonistSakMedTrygdeavgift
            transactionTemplate.executeWithoutResult(status -> opprettEøsPensjonistSakMedTrygdeavgift("MEL-1003"));

            // MEL-1004: opprettEUEOSSak (default: Ikke yrkesaktiv)
            transactionTemplate.executeWithoutResult(status -> opprettEUEOSSak("MEL-1004", Behandlingstema.IKKE_YRKESAKTIV));

            // MEL-1005: opprettEUEOSSak (variant: Pensjonist)
            transactionTemplate.executeWithoutResult(status -> opprettEUEOSSak("MEL-1005", Behandlingstema.PENSJONIST));

            // MEL-1006: opprettUtenforAvtalelandSakMedAarsavregning
            transactionTemplate.executeWithoutResult(status -> opprettUtenforAvtalelandSakMedAarsavregning("MEL-1006"));

            log.info("✅ Suksessfullt initialisert 6 test-saker (MEL-1001 til MEL-1006)");
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
     * Oppretter en FTRL-sak med:
     * 1. Førstegangsbehandling (AVSLUTTET med vedtak "Søknaden er innvilget")
     * 2. Årsavregning (OPPRETTET)
     * <p>
     * Dette matcher frontend-logikken:
     * - opprettUtenforAvtalelandSak()
     * - avsluttBehandling(sak, "Søknaden er innvilget")
     * - opprett Årsavregning på samme sak
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

        // 2. Opprett Førstegangsbehandling (med status AVSLUTTET fra start)
        // Dette er en forenklet tilnærming for testdata - i prod ville behandlingen
        // først vært OPPRETTET, deretter avsluttet via BehandlingService
        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.AVSLUTTET,  // Direkte avsluttet for testdata
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now().minusDays(30), // Opprettet 30 dager siden
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        // 3. Opprett Årsavregning-behandling (åpen)
        behandlingService.nyBehandling(
            savedFagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.ÅRSAVREGNING,
            Behandlingstema.YRKESAKTIV,
            null, null,
            LocalDate.now().minusDays(28),
            Behandlingsaarsaktyper.SØKNAD,
            null
        );

        log.info("✅ {}: opprettUtenforAvtalelandSakMedAarsavregning (2 behandlinger: avsluttet + årsavregning)", saksnummer);
    }
}
