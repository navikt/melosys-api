package no.nav.melosys.tjenester.gui.dto.utpeking;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public class UtpekingsperiodeDto {

    @JsonUnwrapped(suffix = "Dato")
    public PeriodeDto periode;
    private String lovvalgsbestemmelse;
    private String tilleggsbestemmelse;
    private String lovvalgsland;

    public PeriodeDto getPeriode() {
        return periode;
    }

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public void setLovvalgsbestemmelse(String lovvalgsbestemmelse) {
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
    }

    public String getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public void setTilleggsbestemmelse(String tilleggsbestemmelse) {
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

    public String getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(String lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }
}
