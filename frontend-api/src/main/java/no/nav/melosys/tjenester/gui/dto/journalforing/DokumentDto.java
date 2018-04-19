package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.VedleggTittel;

public class DokumentDto {
    private String navn;
    private LocalDate mottattDato;
    private DokumentTittel tittel;
    private List<VedleggTittel> vedleggstitler;
    private String url;

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

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

    public List<VedleggTittel> getVedleggstitler() {
        return vedleggstitler;
    }

    public void setVedleggstitler(List<VedleggTittel> vedleggstitler) {
        this.vedleggstitler = vedleggstitler;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
