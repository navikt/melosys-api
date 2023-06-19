package no.nav.melosys.exception.validering;

import java.util.List;

public class KontrollerFerdigbehandlingDto {
    private List<KontrollfeilDto> kontrollfeilList;

    public KontrollerFerdigbehandlingDto(List<KontrollfeilDto> kontrollfeilList) {
        this.kontrollfeilList = kontrollfeilList;
    }

    public List<KontrollfeilDto> getKontrollfeilList() {
        return kontrollfeilList;
    }

    public void setKontrollfeilList(List<KontrollfeilDto> kontrollfeilList) {
        this.kontrollfeilList = kontrollfeilList;
    }
}
