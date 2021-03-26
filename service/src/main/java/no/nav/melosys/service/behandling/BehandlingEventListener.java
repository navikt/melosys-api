package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.Period;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.BehandlingsfristEndretEvent;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BehandlingEventListener {

    private static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    private final BehandlingService behandlingService;
    @Qualifier("system")
    private final OppgaveService oppgaveService;

    public BehandlingEventListener(BehandlingService behandlingService, OppgaveService oppgaveService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
    }

    @TransactionalEventListener
    public void dokumentBestilt(DokumentBestiltEvent dokumentBestiltEvent) throws IkkeFunnetException {
        if (dokumentBestiltEvent.getProduserbaredokumenter() == Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER) {
            Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(dokumentBestiltEvent.getBehandlingID());
            if (behandling.erAktiv()) {
                behandlingService.oppdaterStatusOgSvarfrist(
                    behandling,
                    Behandlingsstatus.AVVENT_DOK_PART,
                    Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV))
                );
            }
        }
    }

    @TransactionalEventListener
    @Async
    public void behandlingsfristEndret(BehandlingsfristEndretEvent behandlingsfristEndretEvent) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingsfristEndretEvent.getBehandlingId());
        Optional<Oppgave> oppgave = oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
        if (oppgave.isPresent()) {
            oppgaveService.oppdaterOppgave(oppgave.get().getOppgaveId(), OppgaveOppdatering.builder()
                .fristFerdigstillelse(behandlingsfristEndretEvent.getFristFerdigstillelse())
                .build());
        }
    }
}
