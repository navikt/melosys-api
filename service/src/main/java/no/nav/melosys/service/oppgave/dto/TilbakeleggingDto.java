package no.nav.melosys.service.oppgave.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TilbakeleggingDto {

    private Long behandlingID;

    private String begrunnelse;

    @JsonProperty("venterPaaDokumentasjon")
    private boolean venterPåDokumentasjon;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isVenterPåDokumentasjon() {
        return venterPåDokumentasjon;
    }

    public void setVenterPåDokumentasjon(boolean venterPåDokumentasjon) {
        this.venterPåDokumentasjon = venterPåDokumentasjon;
    }
}
