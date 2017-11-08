package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;

public class FagsakOppsummeringDto {

    private Long saksnummer;
    private String fnr;
    private String sammensattNavn;
    @JsonProperty("kjoenn")
    private String kjønn;
    private FagsakType type;
    private FagsakStatus status;
    private LocalDateTime registrertDato;

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public String getKjønn() {
        return kjønn;
    }

    public void setKjønn(String kjønn) {
        this.kjønn = kjønn;
    }

    public FagsakType getType() {
        return type;
    }

    public void setType(FagsakType type) {
        this.type = type;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }
}
