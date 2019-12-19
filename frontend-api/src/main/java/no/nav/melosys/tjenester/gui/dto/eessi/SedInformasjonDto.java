package no.nav.melosys.tjenester.gui.dto.eessi;

import java.time.LocalDate;

import no.nav.melosys.domain.eessi.SedInformasjon;

public class SedInformasjonDto {
    private final String bucId;
    private final String sedId;
    private final LocalDate opprettetDato;
    private final LocalDate sistOppdatert;
    private final String sedType;
    private final String status;
    private final String rinaUrl;

    public SedInformasjonDto(String bucId, String sedId, LocalDate opprettetDato, LocalDate sistOppdatert, String sedType, String status, String rinaUrl) {
        this.bucId = bucId;
        this.sedId = sedId;
        this.opprettetDato = opprettetDato;
        this.sistOppdatert = sistOppdatert;
        this.sedType = sedType;
        this.status = status;
        this.rinaUrl = rinaUrl;
    }

    public static SedInformasjonDto av(SedInformasjon sedInformasjon) {
        return new SedInformasjonDto(
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
