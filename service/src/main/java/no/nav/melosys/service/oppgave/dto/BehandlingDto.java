package no.nav.melosys.service.oppgave.dto;

import java.time.Instant;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingDto {

    private Long behandlingID;
    private Behandlingstyper behandlingstype;
    private Behandlingsstatus behandlingsstatus;
    private Instant sisteOpplysningerHentetDato;
    private boolean erUnderOppdatering;
    private Instant registrertDato;
    private Instant svarFrist;

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

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getSvarFrist() {
        return svarFrist;
    }

    public void setSvarFrist(Instant svarFrist) {
        this.svarFrist = svarFrist;
    }
}
