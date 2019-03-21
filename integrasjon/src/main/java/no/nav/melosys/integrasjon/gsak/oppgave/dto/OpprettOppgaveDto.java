package no.nav.melosys.integrasjon.gsak.oppgave.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

public class OpprettOppgaveDto {
    @JsonProperty("aktoerId")
    private String aktørId;
    private String tilordnetRessurs;
    private String tema;
    private String oppgavetype;
    private String journalpostId;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate aktivDato;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate fristFerdigstillelse;
    private String prioritet;
    private String saksreferanse;
    private String behandlingstype;
    private String behandlingstema;
    private String temagruppe;
    private String tildeltEnhetsnr;


    public LocalDate getAktivDato() {
        return aktivDato;
    }

    public void setAktivDato(LocalDate aktivDato) {
        this.aktivDato = aktivDato;
    }

    public String getTildeltEnhetsnr() {
        return tildeltEnhetsnr;
    }

    public void setTildeltEnhetsnr(String tildeltEnhetsnr) {
        this.tildeltEnhetsnr = tildeltEnhetsnr;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getSaksreferanse() {
        return saksreferanse;
    }

    public void setSaksreferanse(String saksreferanse) {
        this.saksreferanse = saksreferanse;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public void setTilordnetRessurs(String tilordnetRessurs) {
        this.tilordnetRessurs = tilordnetRessurs;
    }

    public String getTemagruppe() {
        return temagruppe;
    }

    public void setTemagruppe(String temagruppe) {
        this.temagruppe = temagruppe;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public void setFristFerdigstillelse(LocalDate fristFerdigstillelse) {
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(String prioritet) {
        this.prioritet = prioritet;
    }

    @Override
    public String toString() {
        return "OpprettOppgaveDto{" +
            "aktørId='" + aktørId + '\'' +
            ", tilordnetRessurs='" + tilordnetRessurs + '\'' +
            ", tema='" + tema + '\'' +
            ", oppgavetype='" + oppgavetype + '\'' +
            ", journalpostId='" + journalpostId + '\'' +
            ", aktivDato=" + aktivDato +
            ", fristFerdigstillelse=" + fristFerdigstillelse +
            ", prioritet='" + prioritet + '\'' +
            ", saksreferanse='" + saksreferanse + '\'' +
            ", behandlingstype='" + behandlingstype + '\'' +
            ", behandlingstema='" + behandlingstema + '\'' +
            ", temagruppe='" + temagruppe + '\'' +
            ", tildeltEnhetsnr='" + tildeltEnhetsnr + '\'' +
            '}';
    }
}
