package no.nav.melosys.service.abac;

import no.nav.melosys.exception.TekniskException;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.stereotype.Service;

@Service
public class FagsakTilgang {
    private FagsakRepository fagsakRepository;
    private final PepAktoerOversetter pep;

    @Autowired
    public FagsakTilgang(FagsakRepository fagsakRepository, PepAktoerOversetter pep) {
        this.fagsakRepository = fagsakRepository;
        this.pep = pep;
    }

    public void sjekk(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
    }

    public void sjekk(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTil(fnr);
    }
}
