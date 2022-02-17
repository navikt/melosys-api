package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent;
import no.nav.melosys.domain.BehandlingsfristEndretEvent;
import no.nav.melosys.domain.brev.MangelbrevSvarfrist;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class BehandlingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BehandlingEventListener.class);

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    public BehandlingEventListener(BehandlingService behandlingService, @Qualifier("system") OppgaveService oppgaveService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @EventListener
    public void dokumentBestilt(DokumentBestiltEvent dokumentBestiltEvent) {
        if (List.of(MELDING_MANGLENDE_OPPLYSNINGER, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER).contains(dokumentBestiltEvent.getProduserbaredokumenter())) {
            var behandling = behandlingService.hentBehandling(dokumentBestiltEvent.getBehandlingID());
            if (behandling.erAktiv()) {
                behandlingService.oppdaterStatusOgSvarfrist(
                    behandling,
                    Behandlingsstatus.AVVENT_DOK_PART,
                    MangelbrevSvarfrist.beregnFristFraDato(Instant.now())
                );
            }
        }
    }

    @EventListener
    @Async
    public void behandlingsfristEndret(BehandlingsfristEndretEvent behandlingsfristEndretEvent) {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingsfristEndretEvent.getBehandlingId());
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
        oppgave.ifPresent(value -> oppgaveService.oppdaterOppgave(
            value.getOppgaveId(),
            OppgaveOppdatering.builder()
                .fristFerdigstillelse(behandlingsfristEndretEvent.getFristFerdigstillelse())
                .build())
        );
    }

    @EventListener
    @Async
    public void behandlingEndret(BehandlingEndretAvSaksbehandlerEvent behandlingEndretAvSaksbehandlerEvent) {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingEndretAvSaksbehandlerEvent.getBehandlingID());
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
        oppgave.ifPresent(value -> {
                String type = behandlingEndretAvSaksbehandlerEvent.getBehandlingstype().getKode();
                String tema = behandlingEndretAvSaksbehandlerEvent.getBehandlingstema().getKode();
                LocalDate frist = behandlingEndretAvSaksbehandlerEvent.getBehandlingsfrist();

                log.info("Oppdaterer oppgave {} med behandlingstype {}, behandlingstema {} og fristFerdigstillelse {}",
                    value.getOppgaveId(), type, tema, frist);

                oppgaveService.oppdaterOppgave(
                    value.getOppgaveId(),
                    OppgaveOppdatering.builder()
                        .behandlingstype(type)
                        .behandlingstema(tema)
                        .fristFerdigstillelse(frist)
                        .build());
            }
        );
    }
}
