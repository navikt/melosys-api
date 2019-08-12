package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktAvklarMyndighet extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AbstraktAvklarMyndighet.class);

    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public AbstraktAvklarMyndighet(BehandlingRepository behandlingRepository,
                                   BehandlingsresultatRepository behandlingsresultatRepository,
                                   UtenlandskMyndighetService utenlandskMyndighetService) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findWithSaksbehandlingById(behandlingID)
            .orElseThrow(() -> new TekniskException("Behandlingsresultat " + behandlingID + " finnes ikke."));

        boolean innvilgelseEllerAnmodningUnntakSkalSendes = prosessinstans.getType() == ProsessType.ANMODNING_OM_UNNTAK
            || behandlingsresultat.erInnvilgelse();

        if (innvilgelseEllerAnmodningUnntakSkalSendes) {
            utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);
        }
    }
}
