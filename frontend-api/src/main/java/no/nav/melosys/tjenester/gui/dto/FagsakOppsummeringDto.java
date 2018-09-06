package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private FagsakType sakstype;
    private BehandlingType behandlingstype;
    private BehandlingStatus behandlingsstatus;
    private Instant opprettetDato;
    private PeriodeDto soknadsperiode;
    private List<String> land;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public FagsakType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakType sakstype) {
        this.sakstype = sakstype;
    }

    public BehandlingType getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(BehandlingType behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public BehandlingStatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(BehandlingStatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
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
}
