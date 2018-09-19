package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BehandlingTilgang {
    private FagsakRepository fagsakRepository;
    private final Pep pep;

    @Autowired
    public BehandlingTilgang(FagsakRepository fagsakRepository, Pep pep) {
        this.fagsakRepository = fagsakRepository;
        this.pep = pep;
    }

    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, IkkeFunnetException {
        List<Fagsak> fagsaker = fagsakRepository.findByBehandlingsId(behandlingsId);
        for (Fagsak fagsak : fagsaker) {
            pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
        }
    }
}
