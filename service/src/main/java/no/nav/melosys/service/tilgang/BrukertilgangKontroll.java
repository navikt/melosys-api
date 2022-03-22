package no.nav.melosys.service.tilgang;

import no.nav.melosys.sikkerhet.abac.Pep;
import org.springframework.stereotype.Service;

@Service
public class BrukertilgangKontroll {
    private final Pep pep;

    public BrukertilgangKontroll(Pep pep) {
        this.pep = pep;
    }

    public void validerTilgangTilAktørID(String aktørID) {
        pep.sjekkTilgangTilAktoerId(aktørID);
    }

    public void validerTilgangTilFolkeregisterIdent(String folkeregisterIdent) {
        pep.sjekkTilgangTilFnr(folkeregisterIdent);
    }
}
