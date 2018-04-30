package no.nav.melosys.service.journalforing.dto;

import java.util.List;

public class FagsakDto {
    private String type;
    private PeriodeDto soknadsperiode;
    private List<String> land;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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
