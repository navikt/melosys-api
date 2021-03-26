package no.nav.melosys.domain.behandling;

import java.time.LocalDate;

public class BehandlingsfristEndretEvent extends BehandlingEvent {
    private final long behandlingId;
    private final LocalDate fristFerdigstillelse;

    public BehandlingsfristEndretEvent(long behandlingId, LocalDate fristFerdigstillelse) {
        super(behandlingId);
        this.behandlingId = behandlingId;
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }
}
