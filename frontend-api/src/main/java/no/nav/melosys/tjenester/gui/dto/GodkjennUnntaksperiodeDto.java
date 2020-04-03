package no.nav.melosys.tjenester.gui.dto;

public class GodkjennUnntaksperiodeDto {
    private boolean varsleUtland;

    public GodkjennUnntaksperiodeDto() {
    }

    public GodkjennUnntaksperiodeDto(boolean varsleUtland) {
        this.varsleUtland = varsleUtland;
    }

    public boolean isVarsleUtland() {
        return varsleUtland;
    }

    public void setVarsleUtland(boolean varsleUtland) {
        this.varsleUtland = varsleUtland;
    }
}
