package no.nav.melosys.service.journalforing.dto;

import no.nav.melosys.service.felles.dto.SoeknadslandDto;

public class FagsakDto {
    private String sakstype;
    private String sakstema;
    private PeriodeDto soknadsperiode;
    private SoeknadslandDto land;

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public SoeknadslandDto getLand() {
        return land;
    }

    public void setLand(SoeknadslandDto land) {
        this.land = land;
    }

    public String getSakstype() {
        return sakstype;
    }

    public void setSakstype(String sakstype) {
        this.sakstype = sakstype;
    }

    public String getSakstema() {
        return sakstema;
    }

    public void setSakstema(String sakstema) {
        this.sakstema = sakstema;
    }
}
