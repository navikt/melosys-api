package no.nav.melosys.service.brev;

import no.nav.melosys.service.brev.muligemottakere.hentmottakere.HentMottakere;
import no.nav.melosys.service.brev.muligemottakere.hentmottakere.HentMottakereRequest;
import no.nav.melosys.service.brev.muligemottakere.hentmottakere.HentMottakereResponse;
import no.nav.melosys.service.brev.utkast.ProduserUtkast;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMottakere hentMottakere;
    private final ProduserUtkast produserUtkast;

    public BrevbestillingFasade(HentMottakere hentMottakere, ProduserUtkast produserUtkast) {
        this.hentMottakere = hentMottakere;
        this.produserUtkast = produserUtkast;
    }

    public HentMottakereResponse hentMuligeMottakere(HentMottakereRequest hentMuligeMottakereRequest) {
        log.debug("hentMuligeMottakere med HentMottakereRequest: {}", hentMuligeMottakereRequest);
        return hentMottakere.hentMuligeMottakere(hentMuligeMottakereRequest);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingRequest brevbestillingRequest) {
        log.debug("produserUtkast med BrevbestillingRequest: {}", brevbestillingRequest);
        return produserUtkast.produserUtkast(behandlingID, brevbestillingRequest);
    }
}

