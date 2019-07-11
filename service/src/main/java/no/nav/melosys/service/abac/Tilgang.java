package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.sikkerhet.abac.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Tilgang {
    private BehandlingService behandlingService;
    private Pep pep;

    @Autowired
    public Tilgang(BehandlingService behandlingService, Pep pep) {
        this.behandlingService = behandlingService;
        this.pep = pep;
    }

    public void sjekkRedigerbar(long behandlingsId) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingsId);

        if(!behandling.erRedigerbar()) {
            throw new FunksjonellException(String.format("Forsøk på å endre en ikke-redigerbar behandling med id %s", behandlingsId));
        }

        sjekk(behandling);
    }

    // Behandling
    public void sjekk(long behandlingsId) throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingsId);

        sjekk(behandling);
    }

    private void sjekk(Behandling behandling) throws SikkerhetsbegrensningException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer aktør = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    // Fagsak
    public void sjekkSak(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        Aktoer aktør = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkFnr(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTilFnr(fnr);
    }
}
