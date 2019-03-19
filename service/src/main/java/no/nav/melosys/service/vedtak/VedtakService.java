package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
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

    @Autowired
    public VedtakService(BehandlingRepository behandlingRepository, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void anmodningOmUnntak(long behandlingID) throws FunksjonellException, TekniskException {
        log.info("Anmodning om unntak for behandling: {}", behandlingID);

        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke sende andmodning om unntak fordi behandling " + behandlingID + " ikke finnes."));
        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling);
        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());

        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_UTL);
        behandlingRepository.save(behandling);
    }

    @Transactional
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType) throws FunksjonellException, TekniskException {
        log.info("Fatter vedtak for behandling: {}", behandlingID);

        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke fatte vedtak fordi behandling " + behandlingID + " ikke finnes."));

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, behandlingsresultatType);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional
    public void endreVedtak(Long behandlingID, Endretperioder endretperiode) throws FunksjonellException, TekniskException {
        log.info("Endrer vedtak for behandling: {}", behandlingID);

        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke endre vedtak fordi behandling " + behandlingID + " ikke finnes."));

        prosessinstansService.opprettProsessinstansOppdaterAvklarteFakta(behandling, endretperiode);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }
}