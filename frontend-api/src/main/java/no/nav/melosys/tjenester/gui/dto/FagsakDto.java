package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakDto {

    private String saksnummer;
    private Long gsakSaksnummer;
    private Fagsakstype sakstype;
    private Fagsaksstatus saksstatus;
    private Instant registrertDato;
    private Instant endretDato;
    private List<BehandlingDto> behandlinger;

    public FagsakDto() {
        this.behandlinger = new ArrayList<>();
    }

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

    public Fagsakstype getSakstype() {
        return sakstype;
    }

    public void setSakstype(Fagsakstype sakstype) {
        this.sakstype = sakstype;
    }

    public Fagsaksstatus getSaksstatus() {
        return saksstatus;
    }

    public void setSaksstatus(Fagsaksstatus saksstatus) {
        this.saksstatus = saksstatus;
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
