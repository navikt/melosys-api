package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public class FattEosVedtakDto extends FattVedtakDto {
    private String fritekst;
    private String fritekstSed;
    private Set<String> mottakerinstitusjoner;
    private String revurderBegrunnelse;

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public String getRevurderBegrunnelse() {
        return revurderBegrunnelse;
    }

    public void setRevurderBegrunnelse(String revurderBegrunnelse) {
        this.revurderBegrunnelse = revurderBegrunnelse;
    }
}
