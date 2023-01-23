package no.nav.melosys.service.brev;

import no.nav.melosys.service.brev.muligemottakere.HentMottakere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFacade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFacade.class);

    private final HentMottakere hentMottakere;

    public BrevbestillingFacade(HentMottakere hentMottakere) {
        this.hentMottakere = hentMottakere;
    }

    public HentMottakere.ResponseData hentMuligeMottakere(HentMottakere.RequestData hentMuligeMottakereRequestData) {
        log.debug("hentMuligeMottakere med requestData: {}", hentMuligeMottakereRequestData);
        return hentMottakere.hentMuligeMottakere(hentMuligeMottakereRequestData);
    }
}

