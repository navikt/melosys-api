package no.nav.melosys.service.brev;

import no.nav.melosys.service.brev.muligemottakere.HentMottakere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMottakere hentMottakere;

    public BrevbestillingFasade(HentMottakere hentMottakere) {
        this.hentMottakere = hentMottakere;
    }

    public HentMottakere.Response hentMuligeMottakere(HentMottakere.Request hentMuligeMottakereRequest) {
        log.debug("hentMuligeMottakere med requestData: {}", hentMuligeMottakereRequest);
        return hentMottakere.hentMuligeMottakere(hentMuligeMottakereRequest);
    }
}

