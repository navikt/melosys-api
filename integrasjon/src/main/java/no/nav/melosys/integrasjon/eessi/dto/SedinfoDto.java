package no.nav.melosys.integrasjon.eessi.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.eessi.SedInformasjon;

public class SedinfoDto {

    private String bucId;
    private String sedId;
    private LocalDate opprettetDato;
    private LocalDate sistOppdatert;
    private String sedType;
    private String status;
    private String rinaUrl;

    public SedinfoDto() {
    }

    public SedinfoDto(String bucId, String sedId, LocalDate opprettetDato, LocalDate sistOppdatert, String sedType, String status, String rinaUrl) {
        this.bucId = bucId;
        this.sedId = sedId;
        this.opprettetDato = opprettetDato;
        this.sistOppdatert = sistOppdatert;
        this.sedType = sedType;
        this.status = status;
        this.rinaUrl = rinaUrl;
    }

    public SedInformasjon tilDomene() {
        return new SedInformasjon(
            bucId,
            sedId,
            opprettetDato,
            sistOppdatert,
            sedType,
            status,
            rinaUrl
        );
    }

    public static SedinfoDto av(SedInformasjon sedInformasjon) {
        return new SedinfoDto(
            sedInformasjon.getBucId(),
            sedInformasjon.getSedId(),
            sedInformasjon.getOpprettetDato(),
            sedInformasjon.getSistOppdatert(),
            sedInformasjon.getSedType(),
            sedInformasjon.getStatus(),
            sedInformasjon.getRinaUrl()
        );
    }

    public String getBucId() {
        return bucId;
    }

    public void setBucId(String bucId) {
        this.bucId = bucId;
    }

    public String getSedId() {
        return sedId;
    }

    public void setSedId(String sedId) {
        this.sedId = sedId;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(LocalDate opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public LocalDate getSistOppdatert() {
        return sistOppdatert;
    }

    public void setSistOppdatert(LocalDate sistOppdatert) {
        this.sistOppdatert = sistOppdatert;
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
