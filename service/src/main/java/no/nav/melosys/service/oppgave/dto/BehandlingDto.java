package no.nav.melosys.service.oppgave.dto;

import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;

public class BehandlingDto {

    private Long behandlingID;
    private BehandlingType type;
    private BehandlingStatus status;
    private LocalDateTime endretDato;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public BehandlingType getType() {
        return type;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }
}
