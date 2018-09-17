package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakDto {

    private String saksnummer;
    private Long gsakSaksnummer;
    private Fagsakstype type;
    private FagsakStatus status;
    private Instant registrertDato;
    private Instant endretDato;
    private List<BehandlingDto> behandlinger;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public Fagsakstype getType() {
        return type;
    }

    public void setType(Fagsakstype type) {
        this.type = type;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
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

    public List<BehandlingDto> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<BehandlingDto> behandlinger) {
        this.behandlinger = behandlinger;
    }
}
