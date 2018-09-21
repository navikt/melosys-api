package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BehandlingTilgang {
    private FagsakRepository fagsakRepository;
    private final PepAktoerOversetter pep;

    @Autowired
    public BehandlingTilgang(FagsakRepository fagsakRepository, PepAktoerOversetter pep) {
        this.fagsakRepository = fagsakRepository;
        this.pep = pep;
    }

    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        List<Fagsak> fagsaker = fagsakRepository.findByBehandlingsId(behandlingsId);
        if (fagsaker.isEmpty()) {
            throw new SikkerhetsbegrensningException("Klarte ikke å finne brukerident fra fagsak knyttet til behandlingid");
        }

        for (Fagsak fagsak : fagsaker) {
            pep.sjekkTilgangTil(fagsak.hentAktørMedRolleType(RolleType.BRUKER));
        }
    }
}
