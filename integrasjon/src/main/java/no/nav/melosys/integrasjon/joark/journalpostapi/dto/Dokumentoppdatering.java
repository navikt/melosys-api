package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Dokumentoppdatering {
    //@JsonProperty("tittel")
    public final String tittel;

    //@JsonProperty("dokumentInfoId")
    public final String dokumentInfoId;

    public Dokumentoppdatering(String dokumentInfoId, String nyTittel) {
        this.dokumentInfoId = dokumentInfoId;
        this.tittel = nyTittel;
    }
}
