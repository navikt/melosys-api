package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;

public class FagsakDto {

    private Long saksnummer;
    private FagsakType type;
    private FagsakStatus status;
    private LocalDateTime registrertDato;
    private List<BehandlingDto> behandlinger;

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public FagsakType getType() {
        return type;
    }

    public void setType(FagsakType type) {
        this.type = type;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public List<BehandlingDto> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<BehandlingDto> behandlinger) {
        this.behandlinger = behandlinger;
    }

    public FagsakDto withBehandlinger(List<BehandlingDto> behandlinger) {
        this.behandlinger = behandlinger;
        return this;
    }
}
