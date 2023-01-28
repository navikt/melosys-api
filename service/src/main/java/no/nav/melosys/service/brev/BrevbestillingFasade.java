package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.service.brev.feature.*;
import no.nav.melosys.service.brev.feature.hentmuligebrevmottakere.HentMuligeBrevmottakereRequestDto;
import no.nav.melosys.service.brev.feature.hentmuligebrevmottakere.HentMuligeBrevmottakereResponseDto;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMuligeBrevmottakere hentMuligeBrevmottakere;
    private final HentMuligeBrevmottakereEtater hentMuligeMottakereEtater;
    private final ProduserUtkast produserUtkast;
    private final ProduserBrev produserBrev;
    private final HentTilgjengeligeEtater hentTilgjengeligeEtater;

    public BrevbestillingFasade(HentMuligeBrevmottakere hentMuligeBrevmottakere,
                                HentMuligeBrevmottakereEtater hentMuligeMottakereEtater,
                                ProduserUtkast produserUtkast,
                                ProduserBrev produserBrev,
                                HentTilgjengeligeEtater hentTilgjengeligeEtater) {
        this.hentMuligeBrevmottakere = hentMuligeBrevmottakere;
        this.hentMuligeMottakereEtater = hentMuligeMottakereEtater;
        this.produserUtkast = produserUtkast;
        this.produserBrev = produserBrev;
        this.hentTilgjengeligeEtater = hentTilgjengeligeEtater;
    }

    public HentMuligeBrevmottakereResponseDto hentMuligeMottakere(HentMuligeBrevmottakereRequestDto hentMuligeBrevmottakereRequestDto) {
        log.debug("hentMuligeMottakere med HentMottakereRequest: {}", hentMuligeBrevmottakereRequestDto);
        return hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMuligeBrevmottakereRequestDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserUtkast med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        return produserUtkast.produserUtkast(behandlingID, brevbestillingDto);
    }

    public void produserBrev(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserBrev med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        produserBrev.produserBrev(behandlingID, brevbestillingDto);
    }

    public List<Etat> hentTilgjengeligeEtater() {
        return hentTilgjengeligeEtater.hentTilgjengeligeEtater();
    }

    public List<Brevmottaker> hentMuligeMottakereEtater(List<String> orgnrEtater) {
        return hentMuligeMottakereEtater.hentMuligeMottakereEtater(orgnrEtater);
    }
}

