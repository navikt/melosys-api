package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_SØKNAD;

@Component
public class OpprettSoeknad implements StegBehandler {
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    public OpprettSoeknad(BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        behandlingsgrunnlagService.opprettSøknad(prosessinstans);
    }
}
