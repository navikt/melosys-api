package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.ProsessSteg;

public class BehandlingHistorikkDto {

    private LocalDateTime dato;
    private Behandlingsstatus status;
    private ProsessSteg steg;
    private String ident;
    private String kommentar;

    public LocalDateTime getDato() {
        return dato;
    }

    public void setDato(LocalDateTime dato) {
        this.dato = dato;
    }

    public Behandlingsstatus getStatus() {
        return status;
    }

    public void setStatus(Behandlingsstatus status) {
        this.status = status;
    }

    public ProsessSteg getSteg() {
        return steg;
    }

    public void setSteg(ProsessSteg steg) {
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
