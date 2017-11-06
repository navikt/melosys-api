package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingSteg;

public class BehandlingHistorikkDto {

    private LocalDateTime dato;
    private BehandlingStatus status;
    private BehandlingSteg steg;
    private String ident;
    private String kommentar;

    public LocalDateTime getDato() {
        return dato;
    }

    public void setDato(LocalDateTime dato) {
        this.dato = dato;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public BehandlingSteg getSteg() {
        return steg;
    }

    public void setSteg(BehandlingSteg steg) {
        this.steg = steg;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getKommentar() {
        return kommentar;
    }

    public void setKommentar(String kommentar) {
        this.kommentar = kommentar;
    }

}
