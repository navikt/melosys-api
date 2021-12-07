package no.nav.melosys.domain.eessi;

import java.time.LocalDate;

public record SedInformasjon(String bucId, String sedId, LocalDate opprettetDato,
                             LocalDate sistOppdatert, String sedType, String status,
                             String rinaUrl) {

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

    public boolean erAvbrutt() {
        return "AVBRUTT".equalsIgnoreCase(status);
    }
}
