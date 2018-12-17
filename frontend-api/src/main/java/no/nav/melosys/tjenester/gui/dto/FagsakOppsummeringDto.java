package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private Fagsakstype sakstype;
    private Fagsaksstatus saksstatus;
    private Behandlingstype behandlingstype;
    private Behandlingsstatus behandlingsstatus;
    private Instant opprettetDato;
    private PeriodeDto soknadsperiode;
    private List<String> land;

    public FagsakOppsummeringDto() {
        this.soknadsperiode = new PeriodeDto();
        this.land = new ArrayList<>();
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Fagsakstype getSakstype() {
        return sakstype;
    }

    public void setSakstype(Fagsakstype sakstype) {
        this.sakstype = sakstype;
    }

    public Fagsaksstatus getSaksstatus() {
        return saksstatus;
    }

    public void setSaksstatus(Fagsaksstatus saksstatus) {
        this.saksstatus = saksstatus;
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
