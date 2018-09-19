package no.nav.melosys.service.datavarehus;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.FagsakDvh;
import no.nav.melosys.domain.datavarehus.*;
import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatavarehusEventObserver {

    private static final Logger log = LoggerFactory.getLogger(DatavarehusEventObserver.class);

    private final DatavarehusRepository datavarehusRepository;

    public DatavarehusEventObserver(DatavarehusRepository datavarehusRepository) {
        this.datavarehusRepository = datavarehusRepository;
    }

    @EventListener(FagsakOpprettetEvent.class)
    public void håndterFagsakLagretEvent(FagsakOpprettetEvent fagsakOpprettetEvent) {
        Fagsak fagsak = fagsakOpprettetEvent.fagsak;
        Aktoer bruker = null;
        Aktoer arbeidsgiver = null;
        Aktoer representant = null;

        try {
            bruker = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
            arbeidsgiver = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
            representant = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        } catch (TekniskException e) {
            log.error("Fagsak med saksnummer " + fagsak.getSaksnummer() + " har ikke unike rolletyper");
        }

        FagsakDvh.Builder builder = new FagsakDvh.Builder()
            .saksnummer(fagsak.getSaksnummer())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(fagsakOpprettetEvent.endretAv)
            .gsakSaksnummer(fagsak.getGsakSaksnummer())
            .fagsakType(fagsak.getType())
            .fagsakStatus(fagsak.getStatus())
            .bruker(bruker)
            .arbeidsgiver(arbeidsgiver)
            .representant(representant)
            .registrertDato(fagsak.getRegistrertDato())
            .endretDato(fagsak.getEndretDato());
        datavarehusRepository.lagre(builder.build());
        log.info("Fagsak med saksnummer " + fagsak.getSaksnummer() + " lagret");
    }

    @EventListener(BehandlingOpprettetEvent.class)
    public void håndterBehandlingLagretEvent(BehandlingOpprettetEvent behandlingOpprettetEvent) {
        Behandling behandling = behandlingOpprettetEvent.behandling;
        BehandlingDvh.Builder builder = new BehandlingDvh.Builder()
            .behandlingId(behandling.getId())
            .saksnummer(behandling.getFagsak().getSaksnummer())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(behandlingOpprettetEvent.endretAv)
            .behandlingStatus(behandling.getStatus())
            .behandlingstype(behandling.getType())
            .registrertDato(behandling.getRegistrertDato())
            .endretDato(behandling.getEndretDato());
        datavarehusRepository.lagre(builder.build());
        log.info("Behandling med behandlingId " + behandling.getId() + " lagret");
    }

}
