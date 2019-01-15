package no.nav.melosys.service.datavarehus;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoerroller;
import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.FagsakDvh;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.DatavarehusRepository;
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
        konverterOgLagFagsakDvh(fagsak, fagsakOpprettetEvent.endretAv);
    }

    @EventListener(FagsakAvsluttetEvent.class)
    public void håndterFagsakAvsluttetEvent(FagsakAvsluttetEvent fagsakAvsluttetEvent) {
        Fagsak fagsak = fagsakAvsluttetEvent.fagsak;
        konverterOgLagFagsakDvh(fagsak, fagsakAvsluttetEvent.endretAv);
    }

    private void konverterOgLagFagsakDvh(Fagsak fagsak, String endretAv) {
        try {
            Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoerroller.BRUKER);
            Aktoer arbeidsgiver = fagsak.hentAktørMedRolleType(Aktoerroller.ARBEIDSGIVER);
            Aktoer representant = fagsak.hentAktørMedRolleType(Aktoerroller.REPRESENTANT);

            FagsakDvh fagsakDvh = new FagsakDvh();
            fagsakDvh.setSaksnummer(fagsak.getSaksnummer());
            fagsakDvh.setFunksjonellTid(LocalDateTime.now());
            fagsakDvh.setEndretAv(endretAv);
            fagsakDvh.setGsakSaksnummer(fagsak.getGsakSaksnummer());
            fagsakDvh.setFagsakStatus(fagsak.getStatus().getKode());
            if (fagsak.getType() != null) {
                fagsakDvh.setFagsakType(fagsak.getType().getKode());
            }
            if (bruker != null) {
                fagsakDvh.setBrukerId(bruker.getAktørId());
            }
            if (arbeidsgiver != null) {
                fagsakDvh.setArbeidsgiverId(arbeidsgiver.getOrgnr());
            }
            if (representant != null) {
                fagsakDvh.setRepresentantId(representant.getOrgnr());
            }
            fagsakDvh.setRegistrertDato(fagsak.getRegistrertDato());
            fagsakDvh.setEndretDato(fagsak.getEndretDato());
            datavarehusRepository.lagre(fagsakDvh);
            log.info("Fagsak med saksnummer " + fagsak.getSaksnummer() + " lagret");
        } catch (TekniskException e) {
            log.error("Fagsak med saksnummer " + fagsak.getSaksnummer() + " ble ikke lagret");
        }
    }

    @EventListener(BehandlingOpprettetEvent.class)
    public void håndterBehandlingLagretEvent(BehandlingOpprettetEvent behandlingOpprettetEvent) {
        Behandling behandling = behandlingOpprettetEvent.behandling;
        konverterOgLagBehandlingDvh(behandling, behandlingOpprettetEvent.endretAv);
    }

    @EventListener(BehandlingAvsluttetEvent.class)
    public void håndterBehandlingAvsluttetEvent(BehandlingAvsluttetEvent behandlingAvttetEvent) {
        Behandling behandling = behandlingAvttetEvent.behandling;
        konverterOgLagBehandlingDvh(behandling, behandlingAvttetEvent.endretAv);
    }

    private void konverterOgLagBehandlingDvh(Behandling behandling, String endretAv) {
        BehandlingDvh behandlingDvh = new BehandlingDvh();
        behandlingDvh.setBehandling(behandling.getId());
        behandlingDvh.setSaksnummer(behandling.getFagsak().getSaksnummer());
        behandlingDvh.setFunksjonellTid(LocalDateTime.now());
        behandlingDvh.setEndretAv(endretAv);
        if (behandling.getStatus() != null) {
            behandlingDvh.setBehandlingStatus(behandling.getStatus().getKode());
        }
        if (behandling.getType() != null) {
            behandlingDvh.setBehandlingstype(behandling.getType().getKode());
        }
        behandlingDvh.setRegistrertDato(behandling.getRegistrertDato());
        behandlingDvh.setEndretDato(behandling.getEndretDato());
        datavarehusRepository.lagre(behandlingDvh);
        log.info("Behandling med id " + behandling.getId() + " lagret");
    }
}
