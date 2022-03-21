package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagSystemFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentSystemService extends DokumentService {

    public DokumentSystemService(BehandlingService behandlingService,
                                 BrevDataService brevDataService,
                                 @Qualifier("system") DoksysFasade dokSysFasade,
                                 BrevmottakerService brevmottakerService,
                                 BrevDataByggerVelger brevDataByggerVelger,
                                 BrevdataGrunnlagSystemFactory brevdataGrunnlagSystemFactory) {
        super(behandlingService, brevDataService, dokSysFasade, brevmottakerService, brevDataByggerVelger, brevdataGrunnlagSystemFactory);
    }
}
