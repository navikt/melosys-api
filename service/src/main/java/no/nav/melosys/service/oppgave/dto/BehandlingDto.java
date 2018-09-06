package no.nav.melosys.service.oppgave.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;

public class BehandlingDto {

    private Long behandlingID;
    private BehandlingType behandlingType;
    private BehandlingStatus behandlingStatus;
    private Instant sisteOpplysningerHentetDato;
    private Instant endretDato;
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

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

    public void setBehandlingStatus(BehandlingStatus behandlingStatus) {
        this.behandlingStatus = behandlingStatus;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public Instant getSisteOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

}
