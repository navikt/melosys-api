package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;


public class BehandlingOversiktDto {
    private Long behandlingID;
    private Behandlingsstatus behandlingsstatus;
    private Behandlingstyper behandlingstype;
    private Behandlingstema behandlingstema;
    private PeriodeDto lovvalgsperiode;
    private SoeknadslandDto land;
    private PeriodeDto soknadsperiode;
    private Instant opprettetDato;
    private Behandlingsresultattyper behandlingsresultattype;
    private Instant svarFrist;

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

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingstema behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public PeriodeDto getLovvalgsperiode() {
        return lovvalgsperiode;
    }

    public void setLovvalgsperiode(PeriodeDto søknadsperiode) {
        this.lovvalgsperiode = søknadsperiode;
    }

    public SoeknadslandDto getLand() {
        return land;
    }

    public void setLand(SoeknadslandDto land) {
        this.land = land;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public Behandlingsresultattyper getBehandlingsresultattype() {
        return behandlingsresultattype;
    }

    public void setBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype) {
        this.behandlingsresultattype = behandlingsresultattype;
    }

    public Instant getSvarFrist() {
        return svarFrist;
    }

    public void setSvarFrist(Instant svarFrist) {
        this.svarFrist = svarFrist;
    }
}
