package no.nav.melosys.service.abac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.Pep;

@Component
public class FagsakTilgang {
    private FagsakRepository fagsakRepository;
    private final Pep pep;

    @Autowired
    public FagsakTilgang(FagsakRepository fagsakRepository, Pep pep) {
        this.fagsakRepository = fagsakRepository;
        this.pep = pep;
    }

    public void sjekk(Fagsak fagsak) throws SikkerhetsbegrensningException, IkkeFunnetException {
        pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
    }

    public void sjekk(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTil(fnr);
    }
}
