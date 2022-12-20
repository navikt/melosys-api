package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Dokumentoppdatering {

    public final String tittel;
    public final String dokumentInfoId;

    public Dokumentoppdatering(String dokumentInfoId, String nyTittel) {
        this.dokumentInfoId = dokumentInfoId;
        this.tittel = nyTittel;
    }
}
