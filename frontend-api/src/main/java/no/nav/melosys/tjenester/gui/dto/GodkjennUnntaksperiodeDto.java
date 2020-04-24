package no.nav.melosys.tjenester.gui.dto;

public class GodkjennUnntaksperiodeDto {
    private final boolean varsleUtland;

    public GodkjennUnntaksperiodeDto(boolean varsleUtland) {
        this.varsleUtland = varsleUtland;
    }

    public boolean isVarsleUtland() {
        return varsleUtland;
    }
}
