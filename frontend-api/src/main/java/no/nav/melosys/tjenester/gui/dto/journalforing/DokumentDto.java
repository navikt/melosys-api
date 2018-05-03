package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDate;

public class DokumentDto {
    private String dokumentID;
    private String tittel;
    private LocalDate mottattDato;

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }
}
