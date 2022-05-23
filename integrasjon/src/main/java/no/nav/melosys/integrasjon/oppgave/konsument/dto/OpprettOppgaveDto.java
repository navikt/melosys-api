package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

public class OpprettOppgaveDto {
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate aktivDato;
    @JsonProperty("aktoerId")
    private String aktørId;
    private String orgnr;
    private String behandlesAvApplikasjon;
    private String behandlingstema;
    private String behandlingstype;
    private String beskrivelse;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate fristFerdigstillelse;
    private String journalpostId;
    private String oppgavetype;
    private String prioritet;
    private String saksreferanse;
    private String tema;
    private String temagruppe;
    private String tildeltEnhetsnr;
    private String tilordnetRessurs;

    public LocalDate getAktivDato() {
        return aktivDato;
    }

    public void setAktivDato(LocalDate aktivDato) {
        this.aktivDato = aktivDato;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public void setBehandlesAvApplikasjon(String behandlesAvApplikasjon) {
        this.behandlesAvApplikasjon = behandlesAvApplikasjon;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public void setFristFerdigstillelse(LocalDate fristFerdigstillelse) {
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public String getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(String prioritet) {
        this.prioritet = prioritet;
    }

    public String getSaksreferanse() {
        return saksreferanse;
    }

    public void setSaksreferanse(String saksreferanse) {
        this.saksreferanse = saksreferanse;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getTemagruppe() {
        return temagruppe;
    }

    public void setTemagruppe(String temagruppe) {
        this.temagruppe = temagruppe;
    }

    public String getTildeltEnhetsnr() {
        return tildeltEnhetsnr;
    }

    public void setTildeltEnhetsnr(String tildeltEnhetsnr) {
        this.tildeltEnhetsnr = tildeltEnhetsnr;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public void setTilordnetRessurs(String tilordnetRessurs) {
        this.tilordnetRessurs = tilordnetRessurs;
    }

    @Override
    public String toString() {
        return "OpprettOppgaveDto{" +
            "aktivDato=" + aktivDato +
            ", aktørId='" + aktørId + '\'' +
            ", orgnr='" + orgnr + '\'' +
            ", behandlesAvApplikasjon='" + behandlesAvApplikasjon + '\'' +
            ", behandlingstema='" + behandlingstema + '\'' +
            ", behandlingstype='" + behandlingstype + '\'' +
            ", beskrivelse='" + beskrivelse + '\'' +
            ", fristFerdigstillelse=" + fristFerdigstillelse +
            ", journalpostId='" + journalpostId + '\'' +
            ", oppgavetype='" + oppgavetype + '\'' +
            ", prioritet='" + prioritet + '\'' +
            ", saksreferanse='" + saksreferanse + '\'' +
            ", tema='" + tema + '\'' +
            ", temagruppe='" + temagruppe + '\'' +
            ", tildeltEnhetsnr='" + tildeltEnhetsnr + '\'' +
            ", tilordnetRessurs='" + tilordnetRessurs + '\'' +
            '}';
    }
}
