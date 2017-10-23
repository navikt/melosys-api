package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;

public class BehandlingOppsummeringDto {

    private Long gsakId;
    private BehandlingStatus status;
    private BehandlingType type;
    private LocalDateTime registrertDato;

    public Long getGsakId() {
        return gsakId;
    }

    public void setGsakId(Long gsakId) {
        this.gsakId = gsakId;
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

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

}
