package no.nav.melosys.service.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.melosys.domain.Behandling.MULIGE_BEHANDLINGSTEMA_SED;
import static no.nav.melosys.domain.Behandling.MULIGE_BEHANDLINGSTEMA_SOKNAD;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;

import javax.transaction.Transactional;

@Service
public class EndreBehandlingstemaService {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    public EndreBehandlingstemaService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    public List<Behandlingstema> hentMuligeBehandlingstema(long behandlingsID) throws IkkeFunnetException{
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsID);
        if (erBehandlingAvSøknad(behandling.getTema()) && validerBehandling(behandling)) {
            return MULIGE_BEHANDLINGSTEMA_SOKNAD;
        } else if (erBehandlingAvSedForespørsler(behandling.getTema()) && validerBehandling(behandling)) {
            return MULIGE_BEHANDLINGSTEMA_SED;
        } else {
            return Collections.emptyList();
        }
    }

    @Transactional
    public void endreBehandlingstemaTilBehandling(long behandlingsID, Behandlingstema nyttTema) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsID);
        if (hentMuligeBehandlingstema(behandlingsID).contains(nyttTema)) {
            behandling.setTema(nyttTema);
            behandlingService.lagre(behandling);
            behandlingsresultatService.tømBehandlingsresultat(behandlingsID);
            oppdaterGosysOppgave(behandling);
        } else {
            throw new FunksjonellException("Ikke mulig å endre behandlingstema");
        }
    }

    private void oppdaterGosysOppgave(Behandling behandling) throws FunksjonellException, TekniskException {
        Optional<Oppgave> oppgave = oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        if (oppgave.isEmpty()){
            throw new FunksjonellException("Finner ikke tilhørende oppgave");
        }
        oppgaveService.oppdaterOppgave(oppgave.get().getOppgaveId(),
            OppgaveOppdatering.builder()
                .behandlingstema(behandling.getTema())
                .build());
    }

    private boolean validerBehandling(Behandling behandling) throws IkkeFunnetException{
        return behandling.erAktiv() && !behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).erArtikkel16MedSendtAnmodningOmUnntak();
    }
}
