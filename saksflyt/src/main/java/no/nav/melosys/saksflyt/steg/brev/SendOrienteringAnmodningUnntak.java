package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

@Component("AnmodningOmUnntakSendBrev")
public class SendOrienteringAnmodningUnntak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendOrienteringAnmodningUnntak.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;

    @Autowired
    public SendOrienteringAnmodningUnntak(BrevBestiller brevBestiller, BehandlingService behandlingService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_ORIENTERING_ANMODNING_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        brevBestiller.bestill(ORIENTERING_ANMODNING_UNNTAK, saksbehandler, Mottaker.av(BRUKER), behandling);
        log.info("Sendt alle brev for anmodning om unntak i behandling {}", behandling.getId());
    }
}
