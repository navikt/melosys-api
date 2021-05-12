package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.SEND_FORVALTNINGSMELDING;

@Component
public class SendForvaltningsmelding implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendForvaltningsmelding.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;

    @Autowired
    public SendForvaltningsmelding(BrevBestiller brevBestiller, BehandlingService behandlingService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return SEND_FORVALTNINGSMELDING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        boolean skalSendesForvaltningsmelding = prosessinstans.getData(SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class, Boolean.FALSE);

        if (prosessinstans.getBehandling().erBehandlingAvSøknad() && skalSendesForvaltningsmelding) {
            Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
            String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
            brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID, saksbehandler, Mottaker.av(BRUKER), behandling);
            log.info("Sendt forvaltningsmelding for behandling {}", prosessinstans.getBehandling().getId());
        } else {
            log.info("Ikke sendt forvaltningsmelding for behandling {}", prosessinstans.getBehandling().getId());
        }
    }
}
