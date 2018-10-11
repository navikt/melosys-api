package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"dokumentID", "tittel", "mottattDato"})
public class DokumentDto {
    private String dokumentID;
    private String tittel;
    private Instant mottattDato;

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public Instant getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(Instant mottattDato) {
        this.mottattDato = mottattDato;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }
}
