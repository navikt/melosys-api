package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.BREVBESTILLING;
import static no.nav.melosys.saksflytapi.domain.ProsessSteg.BESTILL_BREV;

@Component
public class BestillBrev implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(BestillBrev.class);

    private final BrevBestiller brevBestiller;

    public BestillBrev(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return BESTILL_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final var behandling = prosessinstans.getBehandling();
        if (behandling == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }

        final var brevbestilling = prosessinstans.getData(BREVBESTILLING, DoksysBrevbestilling.class);
        if (brevbestilling == null) {
            throw new FunksjonellException("Prosessinstans mangler brevbestilling");
        }

        if (brevbestilling.getMottakere().size() != 1) {
            throw new FunksjonellException("Prosessinstans skal sende brev til én mottaker, fant " + brevbestilling.getMottakere().size());
        }

        brevbestilling.setBehandling(behandling);
        brevBestiller.bestill(brevbestilling);

        log.info("Brev for behandling {} er bestilt", behandling.getId());
    }
}
