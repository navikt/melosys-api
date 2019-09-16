package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideresendService {
    private static final Logger log = LoggerFactory.getLogger(VideresendService.class);

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    @Autowired
    public VideresendService(BehandlingService behandlingService, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void videresend(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        log.info("Videresender søknad for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}