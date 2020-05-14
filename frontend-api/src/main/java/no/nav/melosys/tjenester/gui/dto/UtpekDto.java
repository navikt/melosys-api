package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public class UtpekDto {
    private Set<String> mottakerinstitusjoner;
    private String fritekstSed;

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
}
