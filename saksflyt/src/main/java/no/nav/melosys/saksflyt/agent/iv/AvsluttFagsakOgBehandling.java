package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.datavarehus.BehandlingOpprettetEvent;
import no.nav.melosys.service.datavarehus.FagsakOpprettetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.IV_AVSLUTTBEHANDLING;
import static no.nav.melosys.domain.ProsessSteg.STATUS_BEH_AVSL;

/**
 * Avslutter en fagsak og Behanlding i Melosys.
 *
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *    IV_AVSLUTTBEHANDLING -> STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public AvsluttFagsakOgBehandling(BehandlingRepository behandlingRepository,
                                     FagsakRepository fagsakRepository,
                                     ApplicationEventPublisher applicationEventPublisher) {
        log.info("IverksetteVedtakAvsluttBehandling initialisert");
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_AVSLUTTBEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        String endretAv = prosessinstans.getData(SAKSBEHANDLER);

        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(Fagsaksstatus.AVSLUTTET);
        fagsakRepository.save(fagsak);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);

        applicationEventPublisher.publishEvent(new FagsakOpprettetEvent(fagsak, endretAv));
        applicationEventPublisher.publishEvent(new BehandlingOpprettetEvent(behandling, endretAv));

        prosessinstans.setSteg(STATUS_BEH_AVSL);
    }
}
