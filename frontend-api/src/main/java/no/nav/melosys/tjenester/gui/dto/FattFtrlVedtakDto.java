package no.nav.melosys.tjenester.gui.dto;

public class FattFtrlVedtakDto extends FattVedtakDto {
    private String fritekstInnledning;
    private String fritekstBegrunnelse;
    private String fritekstFamilie;

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public String getFritekstFamilie() {
        return fritekstFamilie;
    }

    public void setFritekstInnledning(String fritekstInnledning) {
        this.fritekstInnledning = fritekstInnledning;
    }

    public void setFritekstBegrunnelse(String fritekstBegrunnelse) {
        this.fritekstBegrunnelse = fritekstBegrunnelse;
    }

    public void setFritekstFamilie(String fritekstFamilie) {
        this.fritekstFamilie = fritekstFamilie;
    }
}
