package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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

    private final BehandlingRepository behandlingRepository;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    private static final String IKKE_FINNES = " ikke finnes.";

    @Autowired
    public VideresendService(BehandlingRepository behandlingRepository, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void videresend(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke videresende søknad fordi behandling " + behandlingID + IKKE_FINNES));
        log.info("Videresender søknad for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}