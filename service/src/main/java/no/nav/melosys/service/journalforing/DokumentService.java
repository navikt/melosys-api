package no.nav.melosys.service.journalforing;

import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!mocking")
public class DokumentService {

    private JoarkFasade joarkFasade;

    @Autowired
    DokumentService(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    public byte[] hentDokument(String journalpostID, String dokumentID) throws SikkerhetsbegrensningException {
        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }
}
