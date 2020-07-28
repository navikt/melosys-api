package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;

public abstract class AbstraktAvklarMyndighet implements StegBehandler {
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public AbstraktAvklarMyndighet(BehandlingService behandlingService,
                                   BehandlingsresultatService behandlingsresultatService,
                                   UtenlandskMyndighetService utenlandskMyndighetService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        boolean innvilgelseEllerAnmodningUnntakSkalSendes = prosessinstans.getType() == ProsessType.ANMODNING_OM_UNNTAK
            || behandlingsresultat.erInnvilgelse();
        boolean søknadSkalVideresendes = behandling.getFagsak().getStatus() == Saksstatuser.VIDERESENDT;

        if (innvilgelseEllerAnmodningUnntakSkalSendes || søknadSkalVideresendes) {
            utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);
        }
    }
}
