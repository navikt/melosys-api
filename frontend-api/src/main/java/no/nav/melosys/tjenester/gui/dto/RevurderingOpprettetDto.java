package no.nav.melosys.tjenester.gui.dto;

public class RevurderingOpprettetDto {

    private final long behandlingID;

    public RevurderingOpprettetDto(long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public long getBehandlingID() {
        return behandlingID;
    }
}
