package no.nav.melosys.service.tilgang;

import no.nav.melosys.sikkerhet.abac.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TilgangService {
    private final Pep pep;

    @Autowired
    public TilgangService(Pep pep) {
        this.pep = pep;
    }

    public void validerTilgangTilAktørID(String aktørID) {
        pep.sjekkTilgangTilAktoerId(aktørID);
    }

    public void validerTilgangTilFolkeregisterIdent(String folkeregisterIdent) {
        pep.sjekkTilgangTilFnr(folkeregisterIdent);
    }
}
