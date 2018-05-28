package no.nav.melosys.service.journalforing.dto;

import java.util.List;

public class FagsakDto {
    private PeriodeDto soknadsperiode;
    private List<String> land;

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }
}
