package no.nav.melosys.tjenester.gui.dto;

public class GodkjennUnntaksperiodeDto {
    private final boolean varsleUtland;
    private final String fritekst;

    // Kreves av jackson
    public GodkjennUnntaksperiodeDto() {
        this.varsleUtland = false;
        this.fritekst = null;
    }

    public GodkjennUnntaksperiodeDto(boolean varsleUtland, String fritekst) {
        this.varsleUtland = varsleUtland;
        this.fritekst = fritekst;
    }

    public boolean isVarsleUtland() {
        return varsleUtland;
    }

    public String getFritekst() {
        return fritekst;
    }
}
