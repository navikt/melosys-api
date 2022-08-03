package no.nav.melosys.service.behandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent;
import no.nav.melosys.domain.BehandlingsfristEndretEvent;
import no.nav.melosys.domain.brev.DokumentasjonSvarfrist;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class BehandlingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BehandlingEventListener.class);

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    public BehandlingEventListener(BehandlingService behandlingService, OppgaveService oppgaveService) {
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
                    DokumentasjonSvarfrist.beregnFristPaaMangelbrevFraDagensDato()
                );
            }
        }
    }

    @EventListener
    @Async
    public void behandlingsfristEndret(BehandlingsfristEndretEvent behandlingsfristEndretEvent) {
        ThreadLocalAccessInfo.executeProcess("behandlingsfristEndret", () -> {
            var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingsfristEndretEvent.getBehandlingId());
            Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
            oppgave.ifPresent(value -> oppgaveService.oppdaterOppgave(
                value.getOppgaveId(),
                OppgaveOppdatering.builder()
                    .fristFerdigstillelse(behandlingsfristEndretEvent.getFristFerdigstillelse())
                    .build())
            );
        });
    }

    @EventListener
    @Async
    public void behandlingEndret(BehandlingEndretAvSaksbehandlerEvent behandlingEndretAvSaksbehandlerEvent) {
        ThreadLocalAccessInfo.executeProcess("behandlingEndret", () -> {
            final var behandling = behandlingService.hentBehandling(behandlingEndretAvSaksbehandlerEvent.getBehandlingID());
            Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
            oppgave.ifPresent(value -> {
                    final Behandlingstyper type = behandlingEndretAvSaksbehandlerEvent.getBehandlingstype();
                    final Behandlingstema tema = behandlingEndretAvSaksbehandlerEvent.getBehandlingstema();

                    final Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(tema, type).build();

                    final LocalDate frist = behandlingEndretAvSaksbehandlerEvent.getBehandlingsfrist();

                    log.info("Oppdaterer oppgave {} med behandlingstype {}, behandlingstema {} og fristFerdigstillelse {}",
                        value.getOppgaveId(), type.getKode(), tema.getKode(), frist);

                    oppgaveService.oppdaterOppgave(
                        value.getOppgaveId(),
                        OppgaveOppdatering.builder()
                            .behandlingstema(behandlingsOppgaveForType.getBehandlingstema())
                            .behandlingstype(behandlingsOppgaveForType.getBehandlingstype())
                            .tema(behandlingsOppgaveForType.getTema())
                            .fristFerdigstillelse(frist)
                            .build());
                }
            );
        });
    }
}
