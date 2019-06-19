package no.nav.melosys.service.unntak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmodningUnntakService {
    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeService.class);

    private final BehandlingRepository behandlingRepository;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    public AnmodningUnntakService(BehandlingRepository behandlingRepository, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntak(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke sende andmodning om unntak fordi behandling " + behandlingID + " finne ikke."));
        log.info("Anmodning om unntak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_UTL);
        behandlingRepository.save(behandling);
        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling);

        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

}
