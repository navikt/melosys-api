package no.nav.melosys.service.behandling;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.*;

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

    @Transactional(readOnly = true)
    public List<Behandlingstema> hentMuligeBehandlingstema(long behandlingsID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsID);
        return hentMuligeBehandlingstema(behandling);
    }

    private List<Behandlingstema> hentMuligeBehandlingstema(Behandling behandling) {
        boolean kanOppdatereBehandlingstema = kanOppdatereBehandlingstema(behandling);
        if (kanOppdatereBehandlingstema && erGyldigBehandlingAvSøknad(behandling.getTema())) {
            return BEHANDLINGSTEMA_SØKNAD;
        } else if (kanOppdatereBehandlingstema && erBehandlingAvSedForespørsler(behandling.getTema())) {
            return BEHANDLINGSTEMA_SED_FORESPØRSEL;
        } else {
            return Collections.emptyList();
        }
    }

    @Transactional
    public void endreBehandlingstemaTilBehandling(long behandlingsID, Behandlingstema nyttTema) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingsID);
        if (hentMuligeBehandlingstema(behandling).contains(nyttTema)) {
            behandling.setTema(nyttTema);
            behandlingService.lagre(behandling);
            behandlingsresultatService.tømBehandlingsresultat(behandlingsID);
            oppdaterOppgave(behandling);
        } else {
            throw new FunksjonellException("Ikke mulig å endre behandlingstema");
        }
    }

    private void oppdaterOppgave(Behandling behandling) {
        Oppgave oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())
            .orElseThrow(() -> new FunksjonellException("Finner ikke tilhørende oppgave"));

        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType()).build();

        oppgaveService.oppdaterOppgave(oppgave.getOppgaveId(),
            OppgaveOppdatering.builder()
                .behandlingstema(behandlingsOppgaveForType.getBehandlingstema())
                .behandlingstype(behandlingsOppgaveForType.getBehandlingstype())
                .tema(behandlingsOppgaveForType.getTema())
                .build());
    }

    private boolean kanOppdatereBehandlingstema(Behandling behandling) {
        return behandling.erAktiv() && behandlingsresultatService.hentBehandlingsresultat(
            behandling.getId()).erIkkeArtikkel16MedSendtAnmodningOmUnntak();
    }
}
