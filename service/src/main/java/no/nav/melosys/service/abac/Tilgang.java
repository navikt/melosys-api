package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoerroller;
import no.nav.melosys.exception.IkkeFunnetException;
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
    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingRepository.findById(behandlingsId)
            .orElseThrow(() -> new IkkeFunnetException(String.format("Klarte ikke å finne behandlingen med id %s.", behandlingsId)));

        Fagsak fagsak = behandling.getFagsak();
        Aktoer aktør = fagsak.hentAktørMedRolleType(Aktoerroller.BRUKER);
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    // Fagsak
    public void sjekkSak(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        Aktoer aktør = fagsak.hentAktørMedRolleType(Aktoerroller.BRUKER);
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkFnr(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTilFnr(fnr);
    }
}
