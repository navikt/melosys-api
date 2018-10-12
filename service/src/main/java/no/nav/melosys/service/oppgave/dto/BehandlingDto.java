package no.nav.melosys.service.oppgave.dto;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Behandlingstype;

public class BehandlingDto {

    private Long behandlingID;
    private Behandlingstype behandlingstype;
    private Behandlingsstatus behandlingsstatus;
    private Instant sisteOpplysningerHentetDato;
    private LocalDate endretDato;
    private boolean erUnderOppdatering;

    public boolean erUnderOppdatering() {
        return erUnderOppdatering;
    }

    public void setErUnderOppdatering(boolean erUnderOppdatering) {
        this.erUnderOppdatering = erUnderOppdatering;
    }

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public Behandlingstype getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstype behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public LocalDate getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDate endretDato) {
        this.endretDato = endretDato;
    }

    public Instant getSisteOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

}
