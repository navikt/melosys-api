package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
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
public class VedtakService {
    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingRepository behandlingRepository;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    private static final String IKKE_FINNES = " ikke finnes.";

    @Autowired
    public VedtakService(BehandlingRepository behandlingRepository, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType, String fritekst) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke fatte vedtak fordi behandling " + behandlingID + IKKE_FINNES));
        log.info("Fatter vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingRepository.save(behandling);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void endreVedtak(Long behandlingID, Endretperiode endretperiode, Behandlingstyper behandlingstype, String fritekst) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke endre vedtak fordi behandling " + behandlingID + IKKE_FINNES));
        log.info("Endrer vedtak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        behandling.setType(behandlingstype);
        behandlingRepository.save(behandling);
        
        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, endretperiode, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}