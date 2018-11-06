package no.nav.melosys.saksflyt.agent.iv;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.jfr.OppdaterJournalpost;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat med vedtaksdato og klagefrist.
 *
 * Transisjoner:
 * IV_OPPDATER_RESULTAT -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterJournalpost.class);

    static final int FRIST_KLAGE_UKER = 6;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        log.info("OppdaterBehandlingsresultat initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_OPPDATER_RESULTAT;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(behandlingID);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultat.setVedtaksdato(Instant.now());
        LocalDate klagefrist = LocalDate.now().plusWeeks(FRIST_KLAGE_UKER);
        behandlingsresultat.setVedtakKlagefrist(klagefrist);
        behandlingsresultatRepository.save(behandlingsresultat);

        prosessinstans.setSteg(IV_OPPDATER_MEDL);
        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Klagefrist: {}", prosessinstans.getId(), klagefrist);
    }
}
