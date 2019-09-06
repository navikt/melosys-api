package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;

public abstract class AbstraktSendUtland extends AbstraktStegBehandler {
    private final EessiService eessiService;
    protected final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendUtland(EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (skalSendeSed(behandlingsresultat)) {
            eessiService.opprettOgSendSed(behandlingID);
        }
    }

    protected abstract boolean skalSendeSed(Behandlingsresultat behandlingsresultat);
}
