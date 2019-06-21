package no.nav.melosys.domain.eessi;

import java.time.LocalDate;

public class SedInformasjon {

    private final String bucId;
    private final String sedId;
    private final LocalDate opprettetDato;
    private final LocalDate sistOppdatert;
    private final String sedType;
    private final String status;
    private final String rinaUrl;

    public SedInformasjon(String bucId, String sedId, LocalDate opprettetDato, LocalDate sistOppdatert, String sedType, String status, String rinaUrl) {
        this.bucId = bucId;
        this.sedId = sedId;
        this.opprettetDato = opprettetDato;
        this.sistOppdatert = sistOppdatert;
        this.sedType = sedType;
        this.status = status;
        this.rinaUrl = rinaUrl;
    }

    public String getBucId() {
        return bucId;
    }

    public String getSedId() {
        return sedId;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public LocalDate getSistOppdatert() {
        return sistOppdatert;
    }

    public String getSedType() {
        return sedType;
    }

    public String getStatus() {
        return status;
    }

    public String getRinaUrl() {
        return rinaUrl;
    }
}
