package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.aktoer.AvklarMyndighetService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentSystemService extends DokumentService {

    @Autowired
    public DokumentSystemService(BehandlingRepository behandlingRepository,
                                 BrevDataService brevDataService, @Qualifier("system") DoksysFasade dokSysFasade,
                                 KontaktopplysningService kontaktopplysningService, BrevDataByggerVelger brevDataByggerVelger,
                                 AvklarteVirksomheterService avklarteVirksomheterService, AvklarMyndighetService avklarMyndighetService) {
        super(behandlingRepository, brevDataService, dokSysFasade, kontaktopplysningService, null, brevDataByggerVelger, avklarteVirksomheterService, avklarMyndighetService);
    }
}