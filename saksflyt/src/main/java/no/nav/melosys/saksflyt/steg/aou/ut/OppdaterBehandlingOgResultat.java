package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat.
 *
 * Transisjoner:
 * AOU_OPPDATER_RESULTAT -> AOU_AVKLAR_MYNDIGHET eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakOppdaterBehandlingOgResultat")
public class OppdaterBehandlingOgResultat implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingOgResultat.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public OppdaterBehandlingOgResultat(BehandlingService behandlingService,
                                        BehandlingsresultatService behandlingsresultatService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_OPPDATER_RESULTAT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();
        String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandling.setEndretAv(saksbehandler);
        behandlingService.lagre(behandling);

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        behandlingsresultat.setEndretAv(saksbehandler);
        behandlingsresultatService.lagre(behandlingsresultat);

        prosessinstans.setSteg(AOU_AVKLAR_MYNDIGHET);
        log.info("Behandling {} fikk status {}.", behandlingID, behandling.getStatus());
        log.debug("Oppdatert behandlingsresultat for prosessinstans {}.", prosessinstans.getId());
    }
}
