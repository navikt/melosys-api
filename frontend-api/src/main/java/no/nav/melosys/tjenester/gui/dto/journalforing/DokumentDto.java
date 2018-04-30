package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"ID", "tittel", "mottattDato"})
public class DokumentDto {
    private String ID;
    private String tittel;
    private LocalDateTime mottattDato;


    @JsonProperty("ID")
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public LocalDateTime getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDateTime mottattDato) {
        this.mottattDato = mottattDato;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }
}
