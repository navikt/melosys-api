package no.nav.melosys.tjenester.gui.dto;

public class FattFtrlVedtakDto extends FattVedtakDto {
    private String fritekstInnledning;
    private String fritekstBegrunnelse;

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public void setFritekstInnledning(String fritekstInnledning) {
        this.fritekstInnledning = fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public void setFritekstBegrunnelse(String fritekstBegrunnelse) {
        this.fritekstBegrunnelse = fritekstBegrunnelse;
    }
}
