package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.REPLIKER_BEHANDLING;


@Component
public class ReplikerBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ReplikerBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    public ReplikerBehandling(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return REPLIKER_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        var behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);

        Optional<Behandling> behandlingBruktForReplikering = fagsakService.hentBehandlingSomErUtgangspunktForRevurdering(fagsak);
        Behandling nyBehandling;

        if (behandlingBruktForReplikering.isPresent()) {
            nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(behandlingBruktForReplikering.get(), behandlingstype);
        } else {
            behandlingBruktForReplikering = Optional.of(fagsak.hentSistOppdatertBehandling());
            nyBehandling = behandlingService.replikerBehandlingUtenBehandlingsresultat(behandlingBruktForReplikering.get(), behandlingstype);
        }

        prosessinstans.setBehandling(nyBehandling);

        fagsakService.lagre(fagsak);

        log.info("Behandling {} replikert og behandling {} har blitt opprettet for {}",
            behandlingBruktForReplikering.get().getId(), nyBehandling.getId(), saksnummer);
    }
}
