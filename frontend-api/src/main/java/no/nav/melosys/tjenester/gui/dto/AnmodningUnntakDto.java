package no.nav.melosys.tjenester.gui.dto;

public class AnmodningUnntakDto {
    private String mottakerinstitusjon;
    private String fritekstSed;

    public String getMottakerinstitusjon() {
        return mottakerinstitusjon;
    }

    public void setMottakerinstitusjon(String mottakerinstitusjon) {
        this.mottakerinstitusjon = mottakerinstitusjon;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }
}
