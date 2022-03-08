package no.nav.melosys.saksflyt.steg.hendelse;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.vedtak.publisering.FattetVedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PubliserVedtak implements StegBehandler {

    private final FattetVedtakService fattetVedtakService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public PubliserVedtak(FattetVedtakService fattetVedtakService, BehandlingsresultatService behandlingsresultatService) {
        this.fattetVedtakService = fattetVedtakService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.PUBLISER_VEDTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();
        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (!resultat.erAvslag()) {
            // Siden dette ikke er i bruk, men vil unngå at det feiler schema validering
            fattetVedtakService.publiserFattetVedtak(behandling.getId());
        }
    }
}
