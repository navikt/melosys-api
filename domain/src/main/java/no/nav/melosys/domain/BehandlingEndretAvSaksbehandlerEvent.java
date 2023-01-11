package no.nav.melosys.domain;

import java.time.LocalDate;

public class BehandlingEndretAvSaksbehandlerEvent extends BehandlingEvent {
    private final LocalDate behandlingsfrist;

    public BehandlingEndretAvSaksbehandlerEvent(long behandlingID, Behandling behandling) {
        super(behandlingID);
        this.behandlingsfrist = behandling.getBehandlingsfrist();
    }

    public LocalDate getBehandlingsfrist() {
        return behandlingsfrist;
    }
}
