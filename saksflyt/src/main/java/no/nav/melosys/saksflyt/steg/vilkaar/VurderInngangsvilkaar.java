package no.nav.melosys.saksflyt.steg.vilkaar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VurderInngangsvilkaar implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(VurderInngangsvilkaar.class);

    private final InngangsvilkaarService inngangsvilkaarService;
    private final BehandlingService behandlingService;

    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService, BehandlingService behandlingService) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.VURDER_INNGANGSVILKÅR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);

        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS && behandling.kanResultereIVedtak()) {
            var søknadsland = behandling.finnSøknadsLand();
            var erUkjenteEllerAlleEosLand = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand;
            var periode = behandling.hentPeriode();

            boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, søknadsland, erUkjenteEllerAlleEosLand, periode);
            log.info("Inngangsvilkår vurdert for behandling {}. kvalifisererForEF_883_2004: {}", behandlingID, kvalifisererForEF_883_2004);
        } else {
            log.info("Inngangsvilkår ikke vurdert for behandling {} med tema {}", behandlingID, behandling.getTema());
        }
    }
}
