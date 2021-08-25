package no.nav.melosys.service.sak;

import java.util.List;

import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;

public class SøknadDto {
    private PeriodeDto periode;
    private SoeknadslandDto land;

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public SoeknadslandDto getLand() {
        return land;
    }

    public void setLand(SoeknadslandDto land) {
        this.land = land;
    }
}
