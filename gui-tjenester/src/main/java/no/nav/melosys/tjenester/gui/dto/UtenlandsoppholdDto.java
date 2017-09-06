package no.nav.melosys.tjenester.gui.dto;

//TODO Hva skal vises må avklares
public class UtenlandsoppholdDto {

    private String land;

    private PeriodeDto periode;

    public void setLand(String land) {
        this.land = land;
    }

    public String getLand() {
        return land;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }
}
