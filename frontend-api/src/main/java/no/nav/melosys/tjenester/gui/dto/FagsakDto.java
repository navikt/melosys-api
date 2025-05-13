package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;

import no.nav.melosys.domain.kodeverk.*;

public class FagsakDto {

    private String saksnummer;
    private Long gsakSaksnummer;
    private Sakstemaer sakstema;
    private Sakstyper sakstype;
    private Saksstatuser saksstatus;
    private Betalingstype betalingsvalg;
    private Instant registrertDato;
    private Instant endretDato;
    private Aktoersroller hovedpartRolle;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public void setSakstema(Sakstemaer saksema) {
        this.sakstema = saksema;
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

    public Betalingstype getBetalingsvalg() { return betalingsvalg; }

    public void setBetalingsvalg(Betalingstype betalingsvalg) { this.betalingsvalg = betalingsvalg; }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public Aktoersroller getHovedpartRolle() {
        return hovedpartRolle;
    }

    public void setHovedpartRolle(Aktoersroller hovedpartRolle) {
        this.hovedpartRolle = hovedpartRolle;
    }
}
