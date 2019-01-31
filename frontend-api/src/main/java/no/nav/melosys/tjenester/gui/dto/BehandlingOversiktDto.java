package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Behandlingstype;

public class BehandlingOversiktDto {
    private Long behandlingID;
    private Behandlingsstatus behandlingsstatus;
    private Behandlingstype behandlingstype;
    private PeriodeDto soknadsperiode;
    private List<String> land;
    private Instant opprettetDato;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public Behandlingstype getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstype behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto søknadsperiode) {
        this.soknadsperiode = søknadsperiode;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
    }


}
