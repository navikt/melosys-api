package no.nav.melosys.integrasjon.gsak.sak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SakDto {

    private Long id;
    private String tema; // https://kodeverkviewer.adeo.no/kodeverk/xml/fagomrade.xml
    private String applikasjon; // Fagsystemkode for applikasjon
    @JsonProperty("fagsakNr")
    private String saksnummer; // Fagsaknr for den aktuelle saken
    @JsonProperty("aktoerId")
    private String aktørId; // Id til aktøren saken gjelder
    private String orgnr; // Orgnr til foretaket saken gjelder
    private String opprettetAv;// Brukerident til den som opprettet saken
    private String opprettetTidspunkt; // Lagres som LocalDateTime i Sak API, men eksponeres som ZonedDateTime

    public SakDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getApplikasjon() {
        return applikasjon;
    }

    public void setApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
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

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public String getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(String opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }
}
