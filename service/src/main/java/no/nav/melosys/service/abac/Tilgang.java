package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.sikkerhet.abac.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Tilgang {

    private BehandlingRepository behandlingRepository;
    private Pep pep;

    @Autowired
    public Tilgang(BehandlingRepository behandlingRepository, Pep pep) {
        this.behandlingRepository = behandlingRepository;
        this.pep = pep;
    }

    // Behandling
    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = behandlingRepository.findOne(behandlingsId);
        if (behandling == null) {
            throw new TekniskException("Klarte ikke å finne brukerident fra fagsak knyttet til behandlingid");
        }

        Fagsak fagsak  = behandling.getFagsak();
        Aktoer aktør = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
    }

    // Fagsak
    public void sjekk(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        Aktoer aktør = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
    }

    public void sjekkFnr(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTilFnr(fnr);
    }
}
