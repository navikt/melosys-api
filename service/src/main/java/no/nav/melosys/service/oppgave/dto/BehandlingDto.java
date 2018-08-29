package no.nav.melosys.service.oppgave.dto;

import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;

public class BehandlingDto {

    private Long behandlingID;
    private BehandlingType behandlingType;
    private BehandlingStatus behandlingStatus;
    private LocalDateTime endretDato;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

    public void setBehandlingStatus(BehandlingStatus behandlingStatus) {
        this.behandlingStatus = behandlingStatus;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }
}
