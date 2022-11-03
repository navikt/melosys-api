package no.nav.melosys.domain.mottatteopplysninger.data;

public class FoedestedOgLand {
    private String foedested;
    private String foedeland;

    public FoedestedOgLand() {}

    public FoedestedOgLand(String foedested, String foedeland) {
        this.foedested = foedested;
        this.foedeland = foedeland;
    }

    public String getFoedested() {
        return foedested;
    }

    public String getFoedeland() {
        return foedeland;
    }
}
