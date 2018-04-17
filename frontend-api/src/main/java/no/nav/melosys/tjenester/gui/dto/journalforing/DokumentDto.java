package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.VedleggTittel;

public class DokumentDto {
    private String navn;
    private LocalDate mottattDato;
    private FagsakType sakstype;
    private DokumentTittel tittel;
    private List<VedleggTittel> vedleggstitler;

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

    public FagsakType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakType sakstype) {
        this.sakstype = sakstype;
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
}
