package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;
import java.util.Properties;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;

public class ProsessinstansDto {
    private long id;
    private long behandlingID;
    private ProsessType type;
    private Properties data;
    private ProsessSteg steg;
    private LocalDateTime registrertDato;
    private LocalDateTime endretDato;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public ProsessType getType() {
        return type;
    }

    public void setType(ProsessType type) {
        this.type = type;
    }

    public Properties getData() {
        return data;
    }

    public void setData(Properties data) {
        this.data = data;
    }

    public ProsessSteg getSteg() {
        return steg;
    }

    public void setSteg(ProsessSteg steg) {
        this.steg = steg;
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
}
