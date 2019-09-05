package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktSendUtland extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AbstraktSendUtland.class);

    protected final BehandlingService behandlingService;
    private final EessiService eessiService;
    protected final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendUtland(BehandlingService behandlingService, EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingService = behandlingService;
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (skalSendeSed(behandlingsresultat)) {
            log.info("Starter sending av SED for behandling {}", behandling.getId());
            eessiService.opprettOgSendSed(behandling, behandlingsresultat);
        }
    }

    protected abstract boolean skalSendeSed(Behandlingsresultat behandlingsresultat);
}
