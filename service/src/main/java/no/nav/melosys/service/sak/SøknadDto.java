package no.nav.melosys.service.sak;

import java.util.List;

import no.nav.melosys.service.journalforing.dto.PeriodeDto;

public class SøknadDto {
    private PeriodeDto periode;
    private List<String> land;

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }
}
