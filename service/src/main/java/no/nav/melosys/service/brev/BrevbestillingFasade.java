package no.nav.melosys.service.brev;

import no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakere;
import no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereRequestDto;
import no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereResponseDto;
import no.nav.melosys.service.brev.produserbrev.ProduserBrev;
import no.nav.melosys.service.brev.utkast.ProduserUtkast;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMuligeBrevmottakere hentMuligeBrevmottakere;
    private final ProduserUtkast produserUtkast;
    private final ProduserBrev produserBrev;

    public BrevbestillingFasade(HentMuligeBrevmottakere hentMuligeBrevmottakere, ProduserUtkast produserUtkast, ProduserBrev produserBrev) {
        this.hentMuligeBrevmottakere = hentMuligeBrevmottakere;
        this.produserUtkast = produserUtkast;
        this.produserBrev = produserBrev;
    }

    public HentMuligeBrevmottakereResponseDto hentMuligeMottakere(HentMuligeBrevmottakereRequestDto hentMuligeBrevmottakereRequestDto) {
        log.debug("hentMuligeMottakere med HentMottakereRequest: {}", hentMuligeBrevmottakereRequestDto);
        return hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMuligeBrevmottakereRequestDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingRequest brevbestillingRequest) {
        log.debug("produserUtkast med BrevbestillingRequest: {}", brevbestillingRequest);
        return produserUtkast.produserUtkast(behandlingID, brevbestillingRequest);
    }

    public void produserBrev(long behandlingID, BrevbestillingRequest brevbestillingRequest) {
        log.debug("produserBrev med BrevbestillingRequest: {}", brevbestillingRequest);
        produserBrev.produserBrev(behandlingID, brevbestillingRequest);
    }
}

