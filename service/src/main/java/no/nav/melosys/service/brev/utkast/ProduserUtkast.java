package no.nav.melosys.service.brev.utkast;

import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.springframework.stereotype.Service;

@Service
public class ProduserUtkast {

    private final DokgenService dokgenService;
    private final DokumentService dokumentService;

    public ProduserUtkast(DokgenService dokgenService, DokumentService dokumentService) {
        this.dokgenService = dokgenService;
        this.dokumentService = dokumentService;
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingRequest brevbestillingRequest) {
        if (dokgenService.erTilgjengeligDokgenmal(brevbestillingRequest.getProduserbardokument())) {
            return dokgenService.produserUtkast(behandlingID, brevbestillingRequest);
        }
        return dokumentService.produserUtkast(behandlingID, brevbestillingRequest);
    }
}
