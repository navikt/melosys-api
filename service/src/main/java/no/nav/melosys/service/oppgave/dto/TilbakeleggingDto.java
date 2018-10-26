package no.nav.melosys.service.oppgave.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TilbakeleggingDto {

    private String oppgaveId;

    private String begrunnelse;

    @JsonProperty("venterPaaDokumentasjon")
    private boolean venterPåDokumentasjon;

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean erVenterPåDokumentasjon() {
        return venterPåDokumentasjon;
    }

    public void setVenterPåDokumentasjon(boolean venterPåDokumentasjon) {
        this.venterPåDokumentasjon = venterPåDokumentasjon;
    }
}
