package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingEndretAvSaksbehandlerEvent extends BehandlingEvent {
    private final Behandlingstyper behandlingstype;
    private final Behandlingstema behandlingstema;
    private final LocalDate behandlingsfrist;

    public BehandlingEndretAvSaksbehandlerEvent(long behandlingID, Behandling behandling) {
        super(behandlingID);
        this.behandlingstype = behandling.getType();
        this.behandlingstema = behandling.getTema();
        this.behandlingsfrist = behandling.getBehandlingsfrist();
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public LocalDate getBehandlingsfrist() {
        return behandlingsfrist;
    }
}
