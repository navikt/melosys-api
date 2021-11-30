package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.dokument.brev.KopiMottaker;

public class FattTrygdeavtaleEllerFtrlVedtakDto extends FattVedtakDto {
    private String fritekstInnledning;
    private String fritekstBegrunnelse;
    private String fritekstEktefelle;
    private String fritekstBarn;
    private List<KopiMottaker> kopiMottakere;

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public String getFritekstEktefelle() {
        return fritekstEktefelle;
    }

    public String getFritekstBarn() {
        return fritekstBarn;
    }

    public List<KopiMottaker> getKopiMottakere() {
        return kopiMottakere;
    }

    public void setFritekstInnledning(String fritekstInnledning) {
        this.fritekstInnledning = fritekstInnledning;
    }

    public void setFritekstBegrunnelse(String fritekstBegrunnelse) {
        this.fritekstBegrunnelse = fritekstBegrunnelse;
    }

    public void setFritekstEktefelle(String fritekstEktefelle) {
        this.fritekstEktefelle = fritekstEktefelle;
    }

    public void setFritekstBarn(String fritekstBarn) {
        this.fritekstBarn = fritekstBarn;
    }

    public void setKopiMottakere(List<KopiMottaker> kopiMottakere) {
        this.kopiMottakere = kopiMottakere;
    }
}
