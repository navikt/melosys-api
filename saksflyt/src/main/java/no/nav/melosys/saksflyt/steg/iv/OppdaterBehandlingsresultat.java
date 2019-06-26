package no.nav.melosys.saksflyt.steg.iv;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat med vedtaksdato og klagefrist.
 *
 * Transisjoner:
 * IV_OPPDATER_RESULTAT -> IV_AVKLAR_MYNDIGHET eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

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

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new FunksjonellException("Behandlingsresultat " + behandlingID + " finnes ikke."));

        if (prosessinstans.getType() == ProsessType.IVERKSETT_VEDTAK) {
            behandlingsresultat.setType(Behandlingsresultattyper.valueOf(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE)));
            behandlingsresultat.setFastsattAvLand(Landkoder.NO);
        }

        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultat.setVedtaksdato(Instant.now());
        LocalDate klagefrist = LocalDate.now().plusWeeks(FRIST_KLAGE_UKER);
        behandlingsresultat.setVedtakKlagefrist(klagefrist);
        behandlingsresultatRepository.save(behandlingsresultat);

        prosessinstans.setSteg(IV_AVKLAR_MYNDIGHET);
        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Klagefrist: {}", prosessinstans.getId(), klagefrist);
    }
}
