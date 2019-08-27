package no.nav.melosys.service.dokument.sed.bygger;

import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SedDataByggerService {
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public SedDataByggerService(LovvalgsperiodeService lovvalgsperiodeService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public SedDataBygger hent(Brevressurser brevressurser) {
        return new SedDataBygger(brevressurser, lovvalgsperiodeService);
    }
}
