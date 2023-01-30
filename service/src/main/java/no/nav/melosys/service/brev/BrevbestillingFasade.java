package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.service.brev.feature.*;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMuligeBrevmottakereComponent hentMuligeBrevmottakereComponent;
    private final HentBrevmottakereEtaterComponent hentBrevmottakereEtaterComponent;
    private final ProduserUtkastComponent produserUtkastComponent;
    private final ProduserBrevComponent produserBrevComponent;
    private final HentTilgjengeligeEtaterComponent hentTilgjengeligeEtaterComponent;

    public BrevbestillingFasade(HentMuligeBrevmottakereComponent hentMuligeBrevmottakereComponent,
                                HentBrevmottakereEtaterComponent hentBrevmottakereEtaterComponent,
                                ProduserUtkastComponent produserUtkastComponent,
                                ProduserBrevComponent produserBrevComponent,
                                HentTilgjengeligeEtaterComponent hentTilgjengeligeEtaterComponent) {
        this.hentMuligeBrevmottakereComponent = hentMuligeBrevmottakereComponent;
        this.hentBrevmottakereEtaterComponent = hentBrevmottakereEtaterComponent;
        this.produserUtkastComponent = produserUtkastComponent;
        this.produserBrevComponent = produserBrevComponent;
        this.hentTilgjengeligeEtaterComponent = hentTilgjengeligeEtaterComponent;
    }

    public HentMuligeBrevmottakereComponent.ResponseDto hentMuligeMottakere(HentMuligeBrevmottakereComponent.RequestDto requestDto) {
        log.debug("hentMuligeMottakere med HentMottakereRequest: {}", requestDto);
        return hentMuligeBrevmottakereComponent.hentMuligeBrevmottakere(requestDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserUtkast med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        return produserUtkastComponent.produserUtkast(behandlingID, brevbestillingDto);
    }

    public void produserBrev(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserBrev med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        produserBrevComponent.produserBrev(behandlingID, brevbestillingDto);
    }

    public List<Etat> hentTilgjengeligeEtater() {
        log.debug("hentTilgjengeligeEtater");
        return hentTilgjengeligeEtaterComponent.hentTilgjengeligeEtater();
    }

    public List<Brevmottaker> hentMuligeBrevmottakereEtater(List<String> orgnrEtater) {
        log.debug("hentMuligeBrevmottakereEtater med orgnr for etater: {}", orgnrEtater);
        return hentBrevmottakereEtaterComponent.hentMuligeBrevmottakereEtater(orgnrEtater);
    }
}

