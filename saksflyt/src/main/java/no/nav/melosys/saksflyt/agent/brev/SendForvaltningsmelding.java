package no.nav.melosys.saksflyt.agent.brev;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;

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

    @Autowired
    public SendForvaltningsmelding(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
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
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID, saksbehandler, Aktoersroller.BRUKER, behandling);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Sendt forvaltningsmelding for prosessinstans {}", prosessinstans.getId());
    }
}
