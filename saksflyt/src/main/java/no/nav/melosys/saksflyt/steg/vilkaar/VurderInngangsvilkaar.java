package no.nav.melosys.saksflyt.steg.vilkaar;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.featuretoggle.ToggleName.IKKEYRKESAKTIV_FLYT;
import static no.nav.melosys.featuretoggle.ToggleName.REGISTRERING_UNNTAK_FRA_MEDLEMSKAP;
import static no.nav.melosys.service.saksbehandling.SaksbehandlingRegler.harTomFlyt;

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

        if (skalVurdereInngangsvilkår(behandling)) {
            var søknadsland = behandling.hentSøknadsLand();
            var erUkjenteEllerAlleEosLand = behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.erUkjenteEllerAlleEosLand;
            var periode = behandling.hentPeriode();

            boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, søknadsland, erUkjenteEllerAlleEosLand, periode);
            log.info("Inngangsvilkår vurdert for behandling {}. kvalifisererForEF_883_2004: {}", behandlingID, kvalifisererForEF_883_2004);
        } else {
            log.info("Inngangsvilkår ikke vurdert for sak {} og behandling {} med sakstype {} og sakstema {}", behandling.getFagsak().getSaksnummer(), behandlingID, behandling.getFagsak().getType(), behandling.getFagsak().getTema());
        }
    }

    private boolean skalVurdereInngangsvilkår(Behandling behandling) {
        return behandling.getFagsak().erSakstypeEøs()
            && !harTomFlyt(behandling, unleash.isEnabled("melosys.folketrygden.mvp"), unleash.isEnabled(IKKEYRKESAKTIV_FLYT), unleash.isEnabled(REGISTRERING_UNNTAK_FRA_MEDLEMSKAP))
            && behandling.kanResultereIVedtak()
            && behandling.harPeriodeOgLand();
    }
}
