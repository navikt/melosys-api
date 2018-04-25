package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDate;

import no.nav.melosys.domain.DokumentTittel;

public class DokumentDto {
    private LocalDate mottattDato;
    private DokumentTittel tittel;

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public DokumentTittel getTittel() {
        return tittel;
    }

    public void setTittel(DokumentTittel tittel) {
        this.tittel = tittel;
    }
}
