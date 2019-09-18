package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;

/**
 * Sender forvaltningsmelding til søker
 *
 * Transisjoner:
 * SEND_FORVALTNINGSMELDING -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendForvaltningsmelding extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendForvaltningsmelding.class);

    private final BrevBestiller brevBestiller;
    
    private final BehandlingService behandlingService;

    @Autowired
    public SendForvaltningsmelding(BrevBestiller brevBestiller, BehandlingService behandlingService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return SEND_FORVALTNINGSMELDING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        // Henter ut behandling med saksopplysninger
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID, saksbehandler, Mottaker.av(BRUKER), behandling);
        
        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Sendt forvaltningsmelding for prosessinstans {}", prosessinstans.getId());
    }
}
