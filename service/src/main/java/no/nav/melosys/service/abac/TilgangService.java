package no.nav.melosys.service.abac;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.abac.Pep;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TilgangService {
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final Pep pep;

    @Autowired
    public TilgangService(FagsakService fagsakService, BehandlingService behandlingService, OppgaveService oppgaveService, Pep pep) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.pep = pep;
    }

    public void sjekkRedigerbarOgTilordnetSaksbehandlerOgTilgang(long behandlingsId) throws FunksjonellException, TekniskException{
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId);
        Oppgave oppgave = oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())
            .orElseThrow(() -> new FunksjonellException(String.format("Finner ikke tilhørende oppgave til behandling %s", behandlingsId)));

        String tilordnetRessurs = oppgave.getTilordnetRessurs();
        if (!(tilordnetRessurs != null && tilordnetRessurs.equalsIgnoreCase(saksbehandler))){
            throw new FunksjonellException(String.format("Forsøk på å endre behandling med id %s som ikke er tilordnet %s", behandlingsId, saksbehandler));
        }

        sjekkRedigerbarOgTilgang(behandlingsId);
    }

    public void sjekkRedigerbarOgTilgang(long behandlingsId) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId);

        sjekkRedigerbarOgTilgang(behandling, behandlingsId);
    }

    public void sjekkRedigerbarOgTilgang(Behandling behandling, long behandlingsId) throws FunksjonellException, TekniskException {
        if(!behandling.erRedigerbar()) {
            throw new FunksjonellException(String.format("Forsøk på å endre en ikke-redigerbar behandling med id %s", behandlingsId));
        }

        sjekkTilgang(behandling);
    }

    // Behandling
    public void sjekkTilgang(long behandlingsId) throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsId);

        sjekkTilgang(behandling);
    }

    private void sjekkTilgang(Behandling behandling) throws SikkerhetsbegrensningException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer aktør = fagsak.hentBruker();
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkSak(String saksnummer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        sjekkSak(fagsakService.hentFagsak(saksnummer));
    }

    // Fagsak
    public void sjekkSak(Fagsak fagsak) throws SikkerhetsbegrensningException, TekniskException {
        Aktoer aktør = fagsak.hentBruker();
        if (aktør != null) {
            pep.sjekkTilgangTilAktoerId(aktør.getAktørId());
        }
    }

    public void sjekkFnr(String fnr) throws SikkerhetsbegrensningException {
        pep.sjekkTilgangTilFnr(fnr);
    }
}
