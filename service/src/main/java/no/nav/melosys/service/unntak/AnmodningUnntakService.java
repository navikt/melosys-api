package no.nav.melosys.service.unntak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmodningUnntakService {
    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakService.class);

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    public AnmodningUnntakService(BehandlingService behandlingService, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntak(long behandlingID) throws FunksjonellException, TekniskException {
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        log.info("Anmodning om unntak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling);
        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntakSvar(long behandlingID) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling);
    }
}
