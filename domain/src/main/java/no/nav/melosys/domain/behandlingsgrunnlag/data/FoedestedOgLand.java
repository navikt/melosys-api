package no.nav.melosys.domain.behandlingsgrunnlag.data;

public class FoedestedOgLand {
    private final String foedested;
    private final String foedeland;

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
