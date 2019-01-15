package no.nav.melosys.service.oppgave.dto;

import java.time.Instant;

import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;

public class BehandlingDto {

    private Long behandlingID;
    private Behandlingstyper behandlingstype;
    private Behandlingsstatus behandlingsstatus;
    private Instant sisteOpplysningerHentetDato;
    private boolean erUnderOppdatering;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public Instant getSisteOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

    public boolean isErUnderOppdatering() {
        return erUnderOppdatering;
    }

    public void setErUnderOppdatering(boolean erUnderOppdatering) {
        this.erUnderOppdatering = erUnderOppdatering;
    }
}
