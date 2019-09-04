package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.ProsessSteg.AOU_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat.
 *
 * Transisjoner:
 * AOU_OPPDATER_RESULTAT -> AOU_AVKLAR_MYNDIGHET eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakOppdaterBehandlingsresultat")
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        log.info("AnmodningOmUnntakOppdaterBehandlingsresultat initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_OPPDATER_RESULTAT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke sende andmodning om unntak fordi behandling " + behandlingID + " ikke finnes."));
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultatRepository.save(behandlingsresultat);

        prosessinstans.setSteg(AOU_AVKLAR_MYNDIGHET);
        log.info("Oppdatert behandlingsresultat for prosessinstans {}.", prosessinstans.getId());
    }
}
