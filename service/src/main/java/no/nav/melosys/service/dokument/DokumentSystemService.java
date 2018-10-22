package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DokumentSystemService extends DokumentService {

    @Autowired
    DokumentSystemService(BehandlingRepository behandlingRepository, BrevDataService brevDataService,
                          @Qualifier("system") DokSysFasade dokSysFasade, JoarkFasade joarkFasade) {
        super(behandlingRepository, brevDataService, dokSysFasade, joarkFasade, null, null);
    }
}