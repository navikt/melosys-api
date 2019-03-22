package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.KontaktopplysningRepository;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentSystemService extends DokumentService {

    @Autowired
    public DokumentSystemService(BehandlingRepository behandlingRepository,
                                 FagsakRepository fagsakRepository, BrevDataService brevDataService,
                                 @Qualifier("system") DoksysFasade dokSysFasade, JoarkFasade joarkFasade,
                                 KontaktopplysningRepository kontaktopplysningRepository, BrevDataByggerVelger brevDataByggerVelger) {
        super(behandlingRepository, fagsakRepository, brevDataService, dokSysFasade, joarkFasade, kontaktopplysningRepository, null, brevDataByggerVelger);
    }
}