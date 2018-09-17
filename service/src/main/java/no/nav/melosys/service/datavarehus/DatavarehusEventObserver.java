package no.nav.melosys.service.datavarehus;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.BehandlingLagretEvent;
import no.nav.melosys.domain.datavarehus.FagsakDvh;
import no.nav.melosys.domain.datavarehus.FagsakLagretEvent;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingDvhRepository;
import no.nav.melosys.repository.FagsakDvhRepository;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatavarehusEventObserver {

    private static final Logger log = LoggerFactory.getLogger(DatavarehusEventObserver.class);

    private final FagsakDvhRepository fagsakDvhRepository;

    private final BehandlingDvhRepository behandlingDvhRepository;

    public DatavarehusEventObserver(FagsakDvhRepository fagsakDvhRepository, BehandlingDvhRepository behandlingDvhRepository) {
        this.fagsakDvhRepository = fagsakDvhRepository;
        this.behandlingDvhRepository = behandlingDvhRepository;
    }

    @EventListener(FagsakLagretEvent.class)
    public void håndterFagsakLagretEvent(FagsakLagretEvent fagsakLagretEvent) {
        Aktoer bruker = null;
        Aktoer arbeidsgiver = null;
        Aktoer representant = null;

        try {
            bruker = fagsakLagretEvent.fagsak.hentAktørMedRolleType(RolleType.BRUKER);
            arbeidsgiver = fagsakLagretEvent.fagsak.hentAktørMedRolleType(RolleType.BRUKER);
            representant = fagsakLagretEvent.fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        } catch (TekniskException e) {
            log.error("Fagsak med saksnummer " + fagsakLagretEvent.fagsak.getSaksnummer() + " har ikke unike rolletyper");
        }

        Fagsak fagsak = fagsakLagretEvent.fagsak;
        FagsakDvh.Builder builder = new FagsakDvh.Builder()
            .saksnummer(fagsak.getSaksnummer())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(SpringSubjectHandler.getInstance().getUserID())
            .gsakSaksnummer(fagsak.getGsakSaksnummer())
            .fagsakType(fagsak.getType())
            .fagsakStatus(fagsak.getStatus())
            .bruker(bruker)
            .arbeidsgiver(arbeidsgiver)
            .representant(representant)
            .registrertDato(fagsak.getRegistrertDato())
            .endretDato(fagsak.getEndretDato());
        fagsakDvhRepository.save(builder.build());
        log.info("Fagsak med saksnummer " + fagsak.getSaksnummer() + " lagret");
    }

    @EventListener(BehandlingLagretEvent.class)
    public void håndterBehandlingLagretEvent(BehandlingLagretEvent behandlingLagretEvent) {
        BehandlingDvh behandlingDvh = new BehandlingDvh();
        behandlingDvh.setBehandlingId(behandlingLagretEvent.behandling.getId());
        behandlingDvhRepository.save(behandlingDvh);
        log.info("Behandling med behandlingId " + behandlingLagretEvent.behandling.getId() + " lagret");
    }

}
