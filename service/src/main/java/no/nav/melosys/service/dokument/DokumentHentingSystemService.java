package no.nav.melosys.service.dokument;

import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class DokumentHentingSystemService extends DokumentHentingService {
    public DokumentHentingSystemService(FagsakService fagsakService, @Qualifier("system") JoarkFasade joarkFasade) {
        super(fagsakService, joarkFasade);
    }
}
