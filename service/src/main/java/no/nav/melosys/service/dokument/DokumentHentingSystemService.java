package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

@Service

public class DokumentHentingSystemService extends DokumentHentingService {
    public DokumentHentingSystemService(FagsakService fagsakService, JoarkFasade joarkFasade) {
        super(fagsakService, joarkFasade);
    }
}
