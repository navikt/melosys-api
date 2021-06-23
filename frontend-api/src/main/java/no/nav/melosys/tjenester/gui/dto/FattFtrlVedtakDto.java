package no.nav.melosys.tjenester.gui.dto;

public class FattFtrlVedtakDto extends FattVedtakDto {
    private String fritekstInnledning;
    private String fritekstBegrunnelse;
    private String fritekstEktefelle;
    private String fritekstBarn;

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
}
