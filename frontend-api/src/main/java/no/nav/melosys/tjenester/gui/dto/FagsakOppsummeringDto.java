package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private String navn;
    private Sakstyper sakstype;
    private Saksstatuser saksstatus;
    private Instant opprettetDato;
    private List<BehandlingOversiktDto> behandlingOversikter;
    private Aktoersroller hovedpartRolle;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }

    public Saksstatuser getSaksstatus() {
        return saksstatus;
    }

    public void setSaksstatus(Saksstatuser saksstatus) {
        this.saksstatus = saksstatus;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public List<BehandlingOversiktDto> getBehandlingOversikter() {
        return behandlingOversikter;
    }

    public void setBehandlingOversikter(List<BehandlingOversiktDto> behandlingOversikter) {
        this.behandlingOversikter = behandlingOversikter;
    }

    public Aktoersroller getHovedpartRolle() {
        return hovedpartRolle;
    }

    public void setHovedpartRolle(Aktoersroller hovedpartRolle) {
        this.hovedpartRolle = hovedpartRolle;
    }
}
