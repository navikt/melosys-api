package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlagFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentSystemService extends DokumentService {

    @Autowired
    public DokumentSystemService(BehandlingService behandlingService,
                                 BrevDataService brevDataService, @Qualifier("system") DoksysFasade dokSysFasade,
                                 BrevmottakerService brevmottakerService, BrevDataByggerVelger brevDataByggerVelger, DokumentdataGrunnlagFactory dokumentdataGrunnlagFactory) {
        super(behandlingService, brevDataService, dokSysFasade, null,  brevmottakerService, brevDataByggerVelger, dokumentdataGrunnlagFactory);
    }
}