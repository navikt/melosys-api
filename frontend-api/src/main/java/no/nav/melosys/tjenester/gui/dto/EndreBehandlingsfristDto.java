package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

public class EndreBehandlingsfristDto {

    private LocalDate behandlingsfrist;

    public LocalDate getBehandlingsfrist() {
        return behandlingsfrist;
    }

    public void setBehandlingsfrist(LocalDate behandlingsfrist) {
        this.behandlingsfrist = behandlingsfrist;
    }
}
