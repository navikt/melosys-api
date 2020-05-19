package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("avsluttFagsakOgBehandlingUnntakFraMedlemskap")
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final FagsakService fagsakService;

    @Autowired
    public AvsluttFagsakOgBehandling(BehandlingsresultatService behandlingsresultatService, FagsakService fagsakService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer());
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(prosessinstans.getBehandling().getId(), Utfallregistreringunntak.GODKJENT);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
