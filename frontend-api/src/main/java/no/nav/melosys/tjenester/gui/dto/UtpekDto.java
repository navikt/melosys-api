package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public class UtpekDto {
    private Set<String> mottakerinstitusjoner;
    private String fritekstSed;
    private String fritekstBrev;

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }

    public String getFritekstBrev() {
        return fritekstBrev;
    }

    public void setFritekstBrev(String fritekstBrev) {
        this.fritekstBrev = fritekstBrev;
    }
}
