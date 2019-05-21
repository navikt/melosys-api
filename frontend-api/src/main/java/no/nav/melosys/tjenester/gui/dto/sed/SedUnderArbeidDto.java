package no.nav.melosys.tjenester.gui.dto.sed;

import java.time.LocalDate;

public class SedUnderArbeidDto {

    private LocalDate opprettetDato;
    private String sedType;
    private String status;
    private String rinaUrl;

    public SedUnderArbeidDto() {
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(LocalDate opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public String getSedType() {
        return sedType;
    }

    public void setSedType(String sedType) {
        this.sedType = sedType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRinaUrl() {
        return rinaUrl;
    }

    public void setRinaUrl(String rinaUrl) {
        this.rinaUrl = rinaUrl;
    }
}
