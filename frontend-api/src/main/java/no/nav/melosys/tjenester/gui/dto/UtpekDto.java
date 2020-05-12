package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

public class UtpekDto {
    private List<String> mottakerinstitusjoner;
    private String fritekstSed;

    public List<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(List<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }
}
