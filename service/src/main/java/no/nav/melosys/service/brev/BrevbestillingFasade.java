package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.service.brev.bestilling.*;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMuligeBrevmottakereService hentMuligeBrevmottakereService;
    private final HentBrevmottakereEtaterService hentBrevmottakereEtaterService;
    private final ProduserUtkastService produserUtkastService;
    private final ProduserBrevService produserBrevService;
    private final HentTilgjengeligeEtaterService hentTilgjengeligeEtaterService;
    private final UtkastBrevService utkastBrevService;

    public BrevbestillingFasade(HentMuligeBrevmottakereService hentMuligeBrevmottakereService,
                                HentBrevmottakereEtaterService hentBrevmottakereEtaterService,
                                ProduserUtkastService produserUtkastService,
                                ProduserBrevService produserBrevService,
                                HentTilgjengeligeEtaterService hentTilgjengeligeEtaterService,
                                UtkastBrevService utkastBrevService) {
        this.hentMuligeBrevmottakereService = hentMuligeBrevmottakereService;
        this.hentBrevmottakereEtaterService = hentBrevmottakereEtaterService;
        this.produserUtkastService = produserUtkastService;
        this.produserBrevService = produserBrevService;
        this.hentTilgjengeligeEtaterService = hentTilgjengeligeEtaterService;
        this.utkastBrevService = utkastBrevService;
    }

    public HentMuligeBrevmottakereService.ResponseDto hentMuligeMottakere(HentMuligeBrevmottakereService.RequestDto requestDto) {
        log.debug("hentMuligeMottakere med HentMottakereRequest: {}", requestDto);
        return hentMuligeBrevmottakereService.hentMuligeBrevmottakere(requestDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserUtkast med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        return produserUtkastService.produserUtkast(behandlingID, brevbestillingDto);
    }

    public void produserBrev(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("produserBrev med BrevbestillingRequest.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        produserBrevService.produserBrev(behandlingID, brevbestillingDto);
    }

    public List<Etat> hentTilgjengeligeEtater() {
        log.debug("hentTilgjengeligeEtater");
        return hentTilgjengeligeEtaterService.hentTilgjengeligeEtater();
    }

    public List<Brevmottaker> hentMuligeBrevmottakereEtater(List<String> orgnrEtater) {
        log.debug("hentMuligeBrevmottakereEtater med orgnr for etater: {}", orgnrEtater);
        return hentBrevmottakereEtaterService.hentMuligeBrevmottakereEtater(orgnrEtater);
    }

    public void slettTilhørendeUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        log.debug("slettTilhørendeUtkast for BrevbestillingDto.produserbardokument: {}", brevbestillingDto.getProduserbardokument());
        utkastBrevService.slettTilhørendeUtkast(behandlingID, brevbestillingDto);
    }
}

