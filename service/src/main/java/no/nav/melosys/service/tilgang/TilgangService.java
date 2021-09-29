package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.abac.Pep;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TilgangService {
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final Pep pep;

    @Autowired
    public TilgangService(FagsakService fagsakService, BehandlingService behandlingService, Pep pep) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.pep = pep;
    }

    public void sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(long behandlingsId) {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId);

        if ( !behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)){
            throw new FunksjonellException(String.format("Forsøk på å endre behandling med id %s som er ikke-redigerbar eller ikke er tilordnet %s", behandlingsId, saksbehandler));
        }

        sjekkTilgang(behandling);
    }

    public void sjekkRedigerbarOgTilgang(long behandlingsId) {
        sjekkRedigerbarOgTilgang(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId));
    }

    public void sjekkRedigerbarOgTilgang(Behandling behandling) {
        if(!behandling.erRedigerbar()) {
            throw new FunksjonellException(String.format("Forsøk på å endre en ikke-redigerbar behandling med id %s", behandling.getId()));
        }

        sjekkTilgang(behandling);
    }

    // Behandling
    public void sjekkTilgang(long behandlingsId) {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId);

        sjekkTilgang(behandling);
    }

    private void sjekkTilgang(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var aktør = fagsak.hentBruker();
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkSak(String saksnummer) {
        sjekkSak(fagsakService.hentFagsak(saksnummer));
    }

    // Fagsak
    public void sjekkSak(Fagsak fagsak) {
        var aktør = fagsak.hentBruker();
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkFnr(String fnr) {
        pep.sjekkTilgangTilFnr(fnr);
    }

    public void validerTilgangTilAktørID(String aktørID) {
        pep.sjekkTilgangTilAktoerId(aktørID);
    }

    public void validerTilgangTilFolkeregisterIdent(String folkeregisterIdent) {
        pep.sjekkTilgangTilFnr(folkeregisterIdent);
    }
}
