package no.nav.melosys.integrasjon.eessi.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.domain.eessi.SedInformasjon;

public class SedinfoDto {

    private String bucId;
    private String sedId;
    private Long opprettetDato;
    private Long sistOppdatert;
    private String sedType;
    private String status;
    private String rinaUrl;

    public SedinfoDto() {
    }

    public SedinfoDto(String bucId, String sedId, Long opprettetDato, Long sistOppdatert, String sedType, String status, String rinaUrl) {
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
            tilLocalDate(opprettetDato),
            tilLocalDate(sistOppdatert),
            sedType,
            status,
            rinaUrl
        );
    }

    public static SedinfoDto av(SedInformasjon sedInformasjon) {
        return new SedinfoDto(
            sedInformasjon.getBucId(),
            sedInformasjon.getSedId(),
            sedInformasjon.getOpprettetDato().toEpochDay(),
            sedInformasjon.getSistOppdatert().toEpochDay(),
            sedInformasjon.getSedType(),
            sedInformasjon.getStatus(),
            sedInformasjon.getRinaUrl()
        );
    }

    private static LocalDate tilLocalDate(Long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
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

    public Long getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Long opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public Long getSistOppdatert() {
        return sistOppdatert;
    }

    public void setSistOppdatert(Long sistOppdatert) {
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
