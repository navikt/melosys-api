package no.nav.melosys.saksflyt.steg.vilkaar;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VurderInngangsvilkaar implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(VurderInngangsvilkaar.class);

    private final InngangsvilkaarService inngangsvilkaarService;
    private final BehandlingService behandlingService;
    private final Unleash unleash;

    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService, BehandlingService behandlingService, Unleash unleash) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.behandlingService = behandlingService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.VURDER_INNGANGSVILKÅR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            if (skalVurdereInngangsvilkår(behandling)) {
                var søknadsland = behandling.hentSøknadsLand();
                var erUkjenteEllerAlleEosLand = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand;
                var periode = behandling.hentPeriode();

                boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, søknadsland, erUkjenteEllerAlleEosLand, periode);
                log.info("Inngangsvilkår vurdert for behandling {}. kvalifisererForEF_883_2004: {}", behandlingID, kvalifisererForEF_883_2004);
            } else {
                log.info("Inngangsvilkår ikke vurdert for behandling {} med tema {}", behandlingID, behandling.getTema());
            }
        } else {
            if (behandling.getFagsak().getType() == Sakstyper.EU_EOS && behandling.kanResultereIVedtakGammel()) {
                var søknadsland = behandling.finnSøknadsLandGammel();
                var erUkjenteEllerAlleEosLand = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand;
                var periode = behandling.hentPeriodeGammel();

                boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, søknadsland, erUkjenteEllerAlleEosLand, periode);
                log.info("Inngangsvilkår vurdert for behandling {}. kvalifisererForEF_883_2004: {}", behandlingID, kvalifisererForEF_883_2004);
            } else {
                log.info("Inngangsvilkår ikke vurdert for behandling {} med tema {}", behandlingID, behandling.getTema());
            }
        }
    }

    private boolean skalVurdereInngangsvilkår(Behandling behandling) {
        return behandling.getFagsak().erSakstypeEøs()
            && !SaksbehandlingRegler.harTomFlyt(behandling)
            && behandling.kanResultereIVedtak()
            && (unleash.isEnabled("melosys.tom_periode_og_land") ? behandling.harPeriodeOgLand() : true);
    }
}
