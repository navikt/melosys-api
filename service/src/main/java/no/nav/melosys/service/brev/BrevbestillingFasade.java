package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.brev.StandardvedleggType;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.service.brev.bestilling.*;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//TODO: Kotlinize
@Service
public class BrevbestillingFasade {
    private static final Logger log = LoggerFactory.getLogger(BrevbestillingFasade.class);

    private final HentMuligeBrevmottakereService hentMuligeBrevmottakereService;
    private final HentBrevmottakereNorskMyndighetService hentBrevmottakereNorskMyndighetService;
    private final ProduserUtkastService produserUtkastService;
    private final ProduserBrevService produserBrevService;
    private final HentTilgjengeligeNorskeMyndigheterService hentTilgjengeligeNorskeMyndigheterService;
    private final DokgenService dokgenService;

    public BrevbestillingFasade(HentMuligeBrevmottakereService hentMuligeBrevmottakereService,
                                HentBrevmottakereNorskMyndighetService hentBrevmottakereNorskMyndighetService,
                                ProduserUtkastService produserUtkastService,
                                ProduserBrevService produserBrevService,
                                HentTilgjengeligeNorskeMyndigheterService hentTilgjengeligeNorskeMyndigheterService,
                                DokgenService dokgenService) {
        this.hentMuligeBrevmottakereService = hentMuligeBrevmottakereService;
        this.hentBrevmottakereNorskMyndighetService = hentBrevmottakereNorskMyndighetService;
        this.produserUtkastService = produserUtkastService;
        this.produserBrevService = produserBrevService;
        this.hentTilgjengeligeNorskeMyndigheterService = hentTilgjengeligeNorskeMyndigheterService;
        this.dokgenService = dokgenService;
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

    public byte[] produserStandardvedleggPdf(StandardvedleggType standardvedleggType) {
       return dokgenService.produserStandardvedlegg(standardvedleggType);
    }

    public List<NorskMyndighet> hentTilgjengeligeNorskeMyndigheter() {
        log.debug("hentTilgjengeligeNorskeMyndigheter");
        return hentTilgjengeligeNorskeMyndigheterService.hentTilgjengeligeNorskeMyndigheter();
    }

    public List<Brevmottaker> hentMuligeBrevmottakereNorskMyndighet(List<String> orgnrNorskMyndighet) {
        log.debug("hentMuligeBrevmottakereNorskMyndighet med orgnr for norske myndigheter: {}", orgnrNorskMyndighet);
        return hentBrevmottakereNorskMyndighetService.hentMuligeBrevmottakereNorskMyndighet(orgnrNorskMyndighet);
    }
}

