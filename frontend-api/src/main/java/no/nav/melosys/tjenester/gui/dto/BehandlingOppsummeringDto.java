package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;

import java.time.Instant;
import java.time.LocalDateTime;

public class BehandlingOppsummeringDto {

    private Long behandlingID;
    private BehandlingStatus status;
    private BehandlingType type;
    private Instant registrertDato;
    private Instant endretDato;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public BehandlingType getType() {
        return type;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

}
