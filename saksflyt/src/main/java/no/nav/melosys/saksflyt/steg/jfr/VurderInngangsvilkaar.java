package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VurderInngangsvilkaar implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(VurderInngangsvilkaar.class);

    private final InngangsvilkaarService inngangsvilkaarService;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    @Autowired
    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService,
                                 FagsakService fagsakService, BehandlingService behandlingService) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        var søknadsland = behandling.finnSøknadsLand();
        var periode = behandling.finnPeriode()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen periode for inngangsvilkårsvurdering for behandling " + behandling.getId()));

        boolean kvalifisererForEF_883_2004  = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandling.getId(), søknadsland, periode);
        fagsakService.oppdaterType(prosessinstans.getBehandling().getFagsak(), kvalifisererForEF_883_2004);
        log.info("Inngangsvilkår vurdert for behandling {}. kvalifisererForEF_883_2004: {}", behandling.getId(), kvalifisererForEF_883_2004);
    }
}
