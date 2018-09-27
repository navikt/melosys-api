package no.nav.melosys.saksflyt.agent;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.*;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.DokumentType.FORVALTNINGSMELDING;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;

/**
 * Sender forvaltningsmelding til søker
 *
 * Transisjoner:
 * SEND_FORVALTNINGSMELDING -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendForvaltningsmelding extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendForvaltningsmelding.class);

    private final DokumentSystemService dokumentService;

    @Autowired
    public SendForvaltningsmelding(DokumentSystemService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return SEND_FORVALTNINGSMELDING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String saksbehandlerId = prosessinstans.getData(SAKSBEHANDLER);
        Behandling behandling = prosessinstans.getBehandling();

        dokumentService.produserDokument(behandling.getId(), FORVALTNINGSMELDING, saksbehandlerId);

        prosessinstans.setSteg(null);
        log.info("Sendt forvaltningsmelding for prosessinstans {}", prosessinstans.getId());
    }
}
