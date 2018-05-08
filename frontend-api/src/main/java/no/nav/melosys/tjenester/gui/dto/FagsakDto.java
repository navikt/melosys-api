package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;

public class FagsakDto {

    private String saksnummer;
    private String gsakSaksnummer; // Denne blir sendt til front-end, men er stadig ikke i bruk
    private FagsakType type;
    private FagsakStatus status;
    private LocalDateTime registrertDato;
    private LocalDateTime endretDato;
    private List<BehandlingDto> behandlinger;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(String gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
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

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public List<BehandlingDto> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<BehandlingDto> behandlinger) {
        this.behandlinger = behandlinger;
    }
}
