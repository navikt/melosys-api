package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.springframework.stereotype.Component;

@Component
public class ProduserUtkastService {

    private final DokgenService dokgenService;
    private final DokumentService dokumentService;

    public ProduserUtkastService(DokgenService dokgenService, DokumentService dokumentService) {
        this.dokgenService = dokgenService;
        this.dokumentService = dokumentService;
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        if (dokgenService.erTilgjengeligDokgenmal(brevbestillingDto.getProduserbardokument())) {
            return dokgenService.produserUtkast(behandlingID, brevbestillingDto);
        }
        return dokumentService.produserUtkast(behandlingID, brevbestillingDto);
    }
}
