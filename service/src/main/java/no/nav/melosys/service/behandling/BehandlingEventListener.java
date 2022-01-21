package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.BehandlingsfristEndretEvent;
import no.nav.melosys.domain.brev.MangelbrevSvarfrist;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class BehandlingEventListener {

    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;

    public BehandlingEventListener(BehandlingService behandlingService, @Qualifier("system") OppgaveService oppgaveService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @EventListener
    public void dokumentBestilt(DokumentBestiltEvent dokumentBestiltEvent) {
        if (List.of(MELDING_MANGLENDE_OPPLYSNINGER, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER).contains(dokumentBestiltEvent.getProduserbaredokumenter())) {
            var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(dokumentBestiltEvent.getBehandlingID());
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
        var behandling = behandlingService.hentBehandling(behandlingsfristEndretEvent.getBehandlingId());
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
        oppgave.ifPresent(value -> oppgaveService.oppdaterOppgave(
            value.getOppgaveId(),
            OppgaveOppdatering.builder()
                .fristFerdigstillelse(behandlingsfristEndretEvent.getFristFerdigstillelse())
                .build())
        );
    }
}
