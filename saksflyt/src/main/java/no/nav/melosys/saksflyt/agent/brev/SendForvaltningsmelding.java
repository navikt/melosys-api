package no.nav.melosys.saksflyt.agent.brev;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.Dokumenttype.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
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
    protected void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        BrevData brevData = new BrevData(prosessinstans.getData(SAKSBEHANDLER));
        dokumentService.produserDokument(behandling.getId(), MELDING_FORVENTET_SAKSBEHANDLINGSTID, brevData);

        prosessinstans.setSteg(null);
        log.info("Sendt forvaltningsmelding for prosessinstans {}", prosessinstans.getId());
    }
}
